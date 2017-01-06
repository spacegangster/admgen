(ns admgen.crud-routes
  (:require [page-renderer.core :refer [render-page]]
            [admgen.form-generator :as formgen]
            [ring.middleware.json             :refer [wrap-json-response]]
            [admgen.panel-generator :as panelgen]
            [admgen.style :as styles]
            [admgen.meta :as ameta]
            [compojure.core :as cj :refer [GET POST DELETE]]))

(defn wrap-ex-report [handler]
  (fn [req]
    (try (handler req)
    (catch Exception ex
      {:status 400
       :body {:status "error" :error (.getMessage ex)}}))))

(defn wrap-api-response [handler]
  (-> handler wrap-ex-report wrap-json-response))

(defn page-response
  ([page-or-page-fn]
   (let [renderable (if (:title page-or-page-fn)
                      page-or-page-fn
                      (page-or-page-fn))]
   {:status 200
    :headers {"Content-Type" "text/html"}
    :body (render-page renderable)}))
  ([page-fn & page-args]
   (page-response (apply page-fn page-args))))

(def bootstrap
  (str "<link rel='stylesheet' href='/bootstrap/css/bootstrap.min.css'>"
       "<script src='/bootstrap/js/bootstrap.min.js'></script>"))

(defn add-urls [route-base emeta]
  (let [codename (name (:codename emeta))
        cat-url  (str route-base "/" codename)]
  (assoc emeta
         :category-url cat-url
         :create-url   (str cat-url "/new")
         )))

(defn mk-crud [& {:keys [admin-name meta-list route-base] :as d}]
  (let [meta-list (map (partial add-urls route-base) meta-list)
        page-edit-item
          (fn [emeta & [post-data]]
            (binding [ameta/crud-url-base route-base]
            (let [{:keys [id title]} post-data
                  meta-name (:singular-title emeta)
                  page-title
                   (if post-data
                     (str "Edit " meta-name " #" id " – " admin-name)
                     (str "New " meta-name " – " admin-name))]
            {:title page-title
             :garden-css styles/root
             :body
             [:body
               [:script {:src "/tinymce/js/tinymce/tinymce.min.js"}]
               [:script {:src "/js2/admin.js"}]
               [:script {:src "/js2/jquery.js"}]
               bootstrap
               [:div.page
                (formgen/gen-form emeta post-data)
                ]
              ]})))

        get-meta
          (fn [codename]
            (let [codename (keyword codename)]
            (first (filter (fn [meta] (= (:codename meta) codename)) meta-list))))
                  
                  show-edit-item
                    (fn [{:keys [params] :as req}]
                      (let [{:keys [entity-name item-id]} params 
                            emeta     (get-meta entity-name)
                            post-data ((:select-fn emeta) item-id)]
                      (page-response page-edit-item emeta post-data)))

        show-create-item
          (fn [{:keys [params] :as req}]
            (let [{:keys [entity-name]} params
                  emeta (get-meta entity-name)]
            (page-response page-edit-item emeta)))

        create-item
          (fn [{:keys [params] :as req}]
            (let [{:keys [entity-name]} params
                  emeta (get-meta entity-name)
                  res (-> params
                          (dissoc :entity-name)
                          ((:create-fn emeta)))
                  item_id (:generated_key res)
                  new-action (str route-base "/" entity-name "/" item_id)]
            {:body {:status "ok", :post_id item_id,  :new_action new-action}}))

        edit-item
          (fn [{:keys [params] :as req}]
            (let [{:keys [entity-name item-id]} params 
                  emeta (get-meta entity-name)
                  res
                    (-> params
                        (dissoc :entity-name :item-id)
                        ((:update-fn emeta)))]
            {:body {:status "ok"}}))

        delete-item
          (fn [{:keys [params] :as req}]
            (let [{:keys [entity-name item-id]} params 
                  emeta (get-meta entity-name)
                  res ((:delete-fn emeta) item-id)]
            {:body {:status "ok"}}))

        table-edit
          (fn [{:keys [params] :as req}]
            (let [action-type (keyword (:action params))
                  params
                    (-> (dissoc params :action)
                        (assoc :item-id (:id params)))
                  r (assoc req :params params)]
            (condp = action-type
              :delete (delete-item r)
              :edit   (edit-item r)
              ; other actions aren't supported
              (let [error-message (str "Action type " (name action-type) " not supported")]
                {:status 400,
                 :body {:status "error", :message error-message}}))))

        show-admin-panel
         ; @param {hash} params
         ;   @param {string} entity-name
         ;   @param {string?} foreign-key
         ;   @param {string?} foreign-id
          (fn [{:keys [params] :as r}]
            (binding [ameta/crud-url-base route-base]
            (let [page-data (panelgen/gen-panel meta-list params)]
            (page-response page-data))))]

  (cj/routes 
    (GET    (str route-base "/:entity-name/new")      req show-create-item)
    (POST   (str route-base "/:entity-name/new")      req (wrap-api-response create-item))
    (POST   (str route-base "/:entity-name/table-edit") req (wrap-api-response table-edit))
    (GET    (str route-base "/:entity-name/:item-id") req show-edit-item)
    (POST   (str route-base "/:entity-name/:item-id") req (wrap-api-response edit-item))
    (DELETE (str route-base "/:entity-name/:item-id") req (wrap-api-response delete-item))
    (GET    (str route-base "/:entity-name")          req show-admin-panel))))
