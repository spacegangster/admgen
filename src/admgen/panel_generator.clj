(ns reanimator.views.panel-generator
  (:require [reanimator.core.data-layer :as dal]
            [reanimator.core.urls :as urls]
            [reanimator.views.admin-components :as comps]
            [clojure.string :as s]
            [page-renderer.util :as u]))

(defn get-entities []
  [
   dal/globals-meta
   dal/codes-meta
   dal/posts-meta
   dal/pages-meta
   dal/brands-meta
   dal/models-meta
   dal/breakages-meta
   dal/phones-meta
   dal/emails-meta
   dal/snippets-meta
   ])


(defn- gen-tab [{:keys [title is-active? category-url] :as entity-data}]
  [:li {:class (if is-active? "active")}
    [:a.panel-tabs-item {:role "presentation" :href category-url}
      (s/upper-case title)]])

(defn gen-tabs [entities active-tab-name]
  [:ul.nav.nav-tabs.panel-tabs
    (for [{:keys [codename] :as item} entities]
    (let [is-active? (= active-tab-name (name codename))
          item (assoc item :is-active? is-active?)]
      (gen-tab item)))])


(defn render-item-row [{:keys [preview-fields edit-url] :as emeta}
                       {:keys [id] :as db-item}]
  (let [item-url ((:gen-edit-url emeta) db-item)]
  [:tr
    (for [fld preview-fields]
    (let [fld-name (:name fld)
          fld-type (:type fld)
          is-link? (= :editlink fld-type)
          attrs (if is-link? {:href item-url} {})
          text  (str (fld-name db-item))
          text  (if (and is-link? (empty? text)) "untitled" text)
          content
            (if is-link?
              [:a attrs text]
              text)]
      [:td content]))]))

(defn gen-table [{:keys [preview-fields list-fn] :as emeta}]
  (let [items (list-fn)]
  [:table.table.table-striped
    [:thead
      [:tr
        (for [item preview-fields]
          [:th (-> item :name name)])]]
    [:tbody (map (partial render-item-row emeta) items)]]))

(def upload-table-form
  [:form {:action (urls/get-url ::urls/admin-upload-table),
          :method "post",
          :enctype "multipart/form-data"}
    (comps/line-file {:label "Upload prices table (.CSV)", :name "table"
                      :desc "Upload all brands, models and prices at once. Only in CSV format"})
    (comps/line-submit)])

(defn render-entity-list-view [{:keys [codename create-url title description] :as entity-meta}]
  [:div.admpanel-pane
   [:div.page-header
     [:h1.admpanel-pane-header (s/capitalize title) " "
       [:small description]]
     [:div
       [:a {:href create-url} (str "+ Add " title)]]
     (gen-table entity-meta)
     (if (= :brands codename)
       upload-table-form)
    ]])

(defn gen-panel [{:keys [tab-name] :as params}]
  (let [entities (get-entities)
        emeta (dal/metas tab-name)]
  {:title "Admin panel"
   :body
    [:body.page.admpanel.container
      (u/make-stylesheet-appender "/bootstrap/css/bootstrap.css")
      [:script {:src "/js2/jquery-2.2.4.js"}]
      [:script {:src "/bootstrap/js/bootstrap.js"}]
      [:script {:src "/js2/admin.js"}]
      (gen-tabs entities tab-name)
      (render-entity-list-view emeta)]}))
