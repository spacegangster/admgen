(ns reanimator.views.form-generator
  (:require [clojure.string :as s])
  (:use reanimator.views.admin-components))


(defn render-field [{:keys [desc field-type default-value] :as field-meta} value]
  (let [field-name (:name field-meta)
        label      (s/capitalize (s/replace (name field-name) #"_" " "))
        is-date?   (= field-type :readonly-date)
        value      (str (or value default-value))
        field-data {:label label, :name field-name, :desc desc, :value value}
        line-renderer
          (condp = field-type
            :readonly  line-readonly
            :text      line-text
            :textarea  line-textarea
            :hidden    line-hidden
            :image     line-image
            :html      line-html
            :checkbox  line-check
            :date      line-date
            :number    line-number
            line-readonly)]
  (line-renderer field-data)))


(defn gen-form [emeta & [instance-data]]
  (let [is-creation? (empty? instance-data)
        {:keys [category-url title fields create-url edit-url]} emeta
        action (if is-creation?
                 create-url
                 ((:gen-edit-url emeta) instance-data))
        title-pref (if is-creation? "New " "Edit ")]
  [:div.form
    [:h1.form-title title-pref title]
    [:form.form-fields {:action action, :method "post", :enctype "multipart/form-data"
                        :data-category-url category-url}
      (for [{:keys [name] :as field-meta} fields]
        (render-field field-meta (name instance-data)))
      (page-submitter category-url)]
    [:form#uploader {:action "/admin/upload-image"}
      [:input#uploader-input {:type "file", :accept "image/*", :name "image"}]]]
    ))
