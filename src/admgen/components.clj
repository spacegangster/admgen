(ns admgen.components)

(defn fmt-date [template date]
  (.format (java.text.SimpleDateFormat. template) date))

(defn uid []
  (str (rand)))

(defn render-select [select-name value opts]
  [:select {:name select-name}
    (for [[opt-val opt-label] opts]
      [:option {:value opt-val, :selected (= value opt-val)} opt-label])])

(defn render-radio [input-name value opts]
  [:div.radio
    (for [[opt-val opt-label] opts]
      [:label
        [:input {:name input-name, :type "radio",
                 :value opt-val, :checked (= value opt-val)}]
        opt-label])])

(defn line [{:keys [required? label desc name value type accept] :as params}]
  [:div.line {:class (if required? "has-warning")}
    [:label.line-label label]
    [:div.line-desc desc]
    [:input.line-value.form-control
      {:required required?, :name name, :type type, :accept accept, :value value }]])

(defn line-select [{:keys [label desc name value opts] :as params}]
  [:div.line
    [:label.line-label label]
    [:div.line-desc desc]
    [:div.line-value
      (render-select name value opts)]])

(defn line-hidden [{:keys [label name value] :as params}]
  [:input {:type "hidden", :name name, :value value}])

(defn line-readonly [{:keys [label value desc] :as params}]
  (if-not (empty? value)
    [:div.line.line--readonly
      [:div.line-label label]
      [:div.line-desc desc]
      [:div.line-value value]]))

(defn line-text [{:keys [label desc name value] :as params}]
  (line (assoc params :type "text")))

(defn line-html
  "Редактор HTML для формы"
  [{:keys [label desc name value css-class]}]
  [:div.line {:class css-class}
    [:label.line-label label]
    [:div.line-desc desc]
    [:input.html-editor-input {:type "hidden", :name name, :value value}]
    [:div.html-editor {:data-editable true, :data-name name} value]])

(defn line-phone [{:keys [label name desc value css-class] :as params}]
  (line (assoc params :type "tel")))

(defn line-file [{:keys [label name desc value css-class] :as params}]
  (line (-> params
            (assoc :type "file")
            (dissoc :value))))

(defn line-image [params]
  (line-file (assoc params :accept "image/*")))

(defn reformat-date [date-str]
  (fmt-date "yyyy-MM-dd" (java.sql.Timestamp/valueOf date-str)))

(defn line-date [{:keys [label name desc value css-class] :as params}]
  (def t value)
  (line (assoc params
               :type "date"
               :value (if (seq value) (reformat-date value) value))))

(defn line-number [{:keys [label name desc value css-class] :as params}]
  (line (assoc params :type "number")))

(defn line-textarea [{:keys [label name desc value css-class] :as params}]
  [:div.line {:class css-class}
    [:label.line-label label]
    [:div.line-desc desc]
    [:textarea.line-value.form-control {:name name} value]])

(defn boolean? [val]
  (= (type val) java.lang.Boolean))

(defn line-check
  "Checkbox line for a form"
  [{:keys [label name value css-class]}]
  (let [value (if (boolean? value)
                value
                (= "true" value))]
  [:div.line.line--check
    [:input {:type "hidden" :class "hidden", :name name, :value "false"}]
    [:label
      [:input {:type "checkbox", :name name, :value "true", :checked value}] " " label]]))

(defn line-submit []
  [:div.line
    [:button.line-value.btn.btn-primary "Submit"]])

(defn page-submitter [category-url]
  [:div.submitter
    [:button.btn.btn-primary.form-submit "Save"]
    [:a.backlink {:href category-url} "Back to admin"]
    [:span.submitter-status]
    [:a.submitter-delete "Delete"]])
