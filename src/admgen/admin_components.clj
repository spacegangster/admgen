(ns reanimator.views.admin-components
  (:require [reanimator.core.util :refer [fmt-date]]))

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

(defn line [{:keys [label desc name value type] :as params}]
  [:div.line
    [:label.line-label label]
    [:div.line-desc desc]
    [:input.line-value.form-control {:name name, :type type, :value value}]])

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

(defn line-file [{:keys [label name desc value css-class] :as params}]
  (line (-> params
            (assoc :type "file")
            (dissoc :value))))

(def line-image line-file)

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

(defn line-check
  "Checkbox line for a form"
  [{:keys [label name value css-class]}]
  [:div.line.line--check
    [:label
      [:input {:type "checkbox", :name name, :value 1, :checked value}] " " label]])

(defn line-submit []
  [:div.line
    [:button.line-value.btn.btn-primary "Submit"]])

(defn page-submitter [category-url]
  [:div.submitter
    [:button.btn.btn-primary.form-submit "Save"]
    [:a {:href category-url} "Back to admin"]
    [:span.submitter-status]
    [:a.submitter-delete "Delete"]])
