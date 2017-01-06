(ns admgen.panel-generator
  (:require [admgen.components :as comps]
            [admgen.style :as styles]
            [clojure.string :as s]
            [page-renderer.util :as u]))

(def ^:dynamic route-base)

(defn- gen-tab [{:keys [title is-active? category-url] :as entity-data}]
  [:li {:class (if is-active? "active")}
    [:a.panel-tabs-item {:role "presentation" :href category-url}
      (s/upper-case title)]])

(defn gen-tabs [entities-meta-list active-tab-name]
  [:ul.nav.nav-tabs.panel-tabs
    (for [{:keys [codename] :as item} entities-meta-list]
    (let [is-active? (= active-tab-name (name codename))
          item (assoc item :is-active? is-active?)]
      (gen-tab item)))])

(defn check-link-type?
  "Checks if preview field type is link
  @param {keyword} link-type"
  [link-type]
  (contains? #{:editlink :children-link} link-type))

(defn- render-preview-cell [field-data db-item emeta]
  (let [fld-name (:name field-data)
        fld-type (:type field-data)
        item-fld-val (str (fld-name db-item))
        suggested-cell-text (if (empty? item-fld-val) "-" item-fld-val)]
  [:td
    (condp = fld-type
      :editlink
        [:a {:href ((:gen-edit-url emeta) db-item)} suggested-cell-text]
      :children-link
        [:a {:href ((:gen-children-url emeta) db-item field-data)} fld-name]
      suggested-cell-text)]))

(defn- render-item-row [{:keys [preview-fields edit-url] :as emeta}
                       {:keys [id] :as db-item}]
  [:tr (for [fld preview-fields]
    (render-preview-cell fld db-item emeta))])

(defn- sort-items [preview-fields items]
  (if-let [order-field (first (filter :orderby preview-fields))]
    (let [asc? (= :asc (:orderby order-field))
          fname (:name order-field)
          sorted-items (sort-by fname items)]
      (if asc?
        sorted-items
        (reverse sorted-items)))
    items))

(defn- check-is-link? [field-type]
  (contains? #{:editlink :children-link} field-type))

(defn render-header-cell [field-data]
  (let [field-name (name (:name field-data))
        editable?
          (and (not= "id" field-name)
               (not (:readonly? field-data))
               (not (check-is-link? (:type field-data))))]
    [:th {:data-name field-name, :data-editable (str editable?)} field-name]))

(defn- gen-table-head [preview-fields]
  [:thead
    [:tr (map render-header-cell preview-fields)]])

(defn- gen-table [{:keys [title preview-fields list-fn] :as emeta} {:keys [foreign-key foreign-id display-name] :as params}]
  (let [wmap (if (seq foreign-key)
               {(keyword foreign-key) foreign-id}
               {})
        emeta (assoc emeta :foreign-key foreign-key, :foreign-id foreign-id, :display-name display-name)
        items (sort-items preview-fields (list-fn wmap))]
  [:table.table.table-striped.table-hover {:class (str "table-" title)}
    (gen-table-head preview-fields)
    [:tbody (map (partial render-item-row emeta) items)]]))

(defn render-entity-list-view
  "@param {hash} addons
   @param {hiccup-vector} addons.post-list-view - an addon to place after the list view"
  [page-header
   {:keys [codename create-url title
           description addons] :as entity-meta} params]
  [:div.admpanel-pane
   [:div.page-header
     [:h1.admpanel-pane-header page-header " "
       [:small description]]
     [:div
       [:a {:href create-url} (str "+ Add " title)]]
     (gen-table entity-meta params)
     (if-let [addons (:post-list-view addons)]
       (for [item addons]
         [:div.mt-40 item]))
    ]])

(def table-edit-scripts
  (list
    [:script {:src "/editablegrid/editablegrid.js"}]
    [:script {:src "/editablegrid/editablegrid_renderers.js"}]
    [:script {:src "/editablegrid/editablegrid_editors.js"}]
    [:script {:src "/editablegrid/editablegrid_validators.js"}]
    [:script {:src "/editablegrid/editablegrid_utils.js"}]
    [:script {:src "/editablegrid/editablegrid_charts.js"}]
    [:script {:src "/js2/admin-table-edit.js"}]))

(def table-edit-scripts2
  (list
    [:script {:src "/jquery-tabledit/jquery.tabledit.js"}]
    [:script {:src "/js2/admin-table-edit2.js"}]))


(defn gen-panel [entities-meta-list {:keys [entity-name foreign-key foreign-id
                                            display-name] :as params}]
  (let [filter-fn (fn [{:keys [codename] :as meta}]
                    (= codename (keyword entity-name)))
        emeta (first (filter filter-fn entities-meta-list))
        page-header (str (s/capitalize (:title emeta))
                         (if display-name
                           (str " : " display-name)
                           display-name))
        page-title (str "Admin panel : " page-header)]
  {:title page-title
   :garden-css styles/root-panel
   :body
    [:body.page.admpanel.container-fluid
      (u/make-stylesheet-appender "/bootstrap/css/bootstrap.css")
      [:script {:src "/js2/jquery.js"}]
      [:script {:src "/bootstrap/js/bootstrap.js"}]
      [:script {:src "/js2/admin.js"}]
      ;
     ;table-edit-scripts
      table-edit-scripts2
      (gen-tabs entities-meta-list entity-name)
      (render-entity-list-view page-header emeta params)]}))
