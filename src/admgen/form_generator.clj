(ns admgen.form-generator
  (:require [clojure.string :as s]
            [admgen.meta :as ameta]
            [admgen.components :as c]))


(defn render-field [{:keys [desc field-type default-value opts] :as field-meta} value]
  (let [field-name (:name field-meta)
        label      (s/capitalize (s/replace (name field-name) #"_" " "))
        is-date?   (= field-type :readonly-date)
        value      (str (or value default-value))
        field-data {:required? (:required? field-meta)
                    :label label,
                    :name field-name,
                    :desc desc, :value value, :opts opts}
        line-renderer
          (condp = field-type
            :readonly  c/line-readonly
            :text      c/line-text
            :phone     c/line-phone
            :textarea  c/line-textarea
            :db-select c/line-select
            :hidden    c/line-hidden
            :image     c/line-image
            :html      c/line-html
            :checkbox  c/line-check
            :date      c/line-date
            :number    c/line-number
            c/line-readonly)]
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
      (for [{field-name :name  :as field-meta} fields]
        (render-field ((:autoexpand-field emeta) field-meta)
                      (field-name instance-data)))
      (c/page-submitter category-url)]
    [:form#uploader {:action "/admin/upload-image"}
      [:input#uploader-input {:type "file", :accept "image/*", :name "image"}]]]
    ))
