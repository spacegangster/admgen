(ns admgen.meta
  (:require [korma.core :as kc]
            [admgen.util :as u])
  (:import java.sql.Timestamp
           java.time.format.DateTimeFormatter
           java.time.LocalDate))


(def ^:dynamic crud-url-base "/admin/crud")

(defn fld
  "@param {keyword} field-name
   @param {?string} desc - field description
   @param {?keyword} ftype - field rich type (defines display and edit rules)"
  [field-name & [desc ftype default-value & {:keys [persistent?] :as o}]]
  (let [ftype (or ftype :text)
        can-be-persistent?
          (and (not= :readonly ftype)
               (not (contains? #{:created :updated} field-name)))
        persistent?
          (if (nil? persistent?)
            can-be-persistent?
            persistent?)
        ]
  (merge o
  {:name field-name,
   :persistent? persistent?
   :desc (or desc "")
   :field-type ftype
   :default-value default-value})))

(defn fld2 [field-name desc & {:keys [default persistent?] :as opts}]
  (let [ftype (or (:type opts) :text)
        can-be-persistent?
          (and (not= :readonly ftype)
               (not (contains? #{:created :updated} field-name)))
        persistent?
          (if (nil? persistent?)
            can-be-persistent?
            persistent?)
        ]
  (merge opts
  {:name field-name,
   :required? (or (:required? opts) false)
   :persistent? persistent?
   :desc (or desc "")
   :field-type ftype
   :default-value default})))



(defn preview-fld [name & [type & {:as opts}]]
  (merge opts {:name name, :type type}))
(def prv preview-fld)

(defn timestamp [millis]
  (java.sql.Timestamp. millis))

(defn now-timestamp []
  (timestamp (System/currentTimeMillis)))

(defn try-parse-number [num-str]
  (try
    (read-string num-str)
  (catch Exception e
    nil)))

(defn get-coercer [field-type]
  (condp = field-type
    :checkbox #(if (= "false" %) false true) ; TODO use more pedantic type name, like boolean?
    :number
     (fn [int-str]
       (if (seq int-str)
         (try-parse-number int-str)
         nil))
    :date
     (fn [date-str]
       (if (seq date-str)
         (Timestamp/valueOf (.atStartOfDay (LocalDate/parse date-str)))))
    identity))

(defn coerce-fields [item fields-info]
  (if-let [{:keys [field-type name] :as field-info} (first fields-info)]
    (let [updated-item
           (if (name item) ; если поля нет в редактируемой сущности - не надо его туда добавлять
             (update item name (get-coercer field-type))
             item)]
      (recur updated-item
             (next fields-info)))
    item))


(defn mkmeta
  "Produces a hash with related functions
   @param {hashmap} list-cond - a map with conditions for where clause"
  [{:keys [list-cond codename table title fields pre-write-fn] :as entity-meta}]
  (let [codename     (name (or codename table))
        korma-root   (kc/create-entity (name table))
        pre-write-fn (or pre-write-fn identity)
        list-cond    (or list-cond {})
        has-updated? (first (filter #(= :updated (:name %)) fields))
        bump-updated (if has-updated?
                       #(assoc % :updated (now-timestamp))
                       identity)
        allowed-fields (filter :persistent? fields)
        allowed-keys (->> allowed-fields (map :name) set)

        select-fn
           (fn [item-id]
             (first (kc/select korma-root (kc/where {:id item-id}))))

        update-id
          (fn [item]
            (if (empty? (:id item))
              (dissoc item :id)
              (update item :id read-string)))

        sanitize-fn
          (fn [item]
            (-> item
                update-id
                (select-keys allowed-keys)
                (coerce-fields allowed-fields)))

        prepare-fn (comp bump-updated sanitize-fn pre-write-fn)]

  (assoc entity-meta

         :autoexpand-field
           ; hook for select and other fields that need db lookups before render
           ; does arbitrary modifications of field metadata
           (fn [{:keys [field-type table-name select-label select-value] :as field-meta}]
             (condp = field-type
               :db-select
                 (let [table-items (doall (kc/select (kc/create-entity table-name)))
                       item->option
                         (fn [item] [(str (item select-value)) (item select-label)])
                       opts (map item->option table-items)
                        ]
                   (assoc field-meta :opts opts))
               field-meta))

         :singular-title (subs title 0 (dec (.length title)))

         :select-fn select-fn

         :create-fn
           (fn [item]
             (kc/insert korma-root
               (kc/values (prepare-fn item))))

         :update-fn
           (fn [{:keys [id] :as item}]
             (kc/update korma-root
               (kc/set-fields (prepare-fn item))
               (kc/where {:id id})))

         :delete-fn
           (fn [item-id]
             (kc/delete korma-root (kc/where {:id item-id})))

         :gen-children-url
           (fn [{:keys [id] :as item}
                {:keys [child-table foreign-key self-name] :as field-meta}]
             (str crud-url-base "/" (name child-table) "?"
                  (u/form-encode {:foreign-key (name foreign-key)
                                  :foreign-id id
                                  :display-name (self-name (select-fn id))})))

         :gen-edit-url
           (fn [{:keys [id] :as item}]
             (str crud-url-base "/" codename "/" id))

         :korma-root korma-root
         :codename   (keyword codename)
         :create-url (str crud-url-base "/" codename "/new")
         :edit-url   (str crud-url-base "/" codename "/{id}")
         :list-fn
           (fn [& [wmap]]
             (kc/select korma-root (kc/where (merge wmap list-cond))))
         )))


(defmacro defx [symname entity-meta]
  `(def ~symname (mkmeta ~entity-meta)))
