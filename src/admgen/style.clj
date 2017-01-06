(ns admgen.style
  (:require [page-renderer.styles :refer [mini-reset]]))


(def dim-page-max-width "900px")

(def post-list
  [[:.post-list
     {:padding "20vh 0"}
     [:&-add
       {}]
     [:&-items
       {:margin-top :2em}]]
   [:.posts-item
     {:padding ".3em .6em"}
     ["&:nth-child(even)"
       {:background "hsl(0, 20%, 95%)"}]
     [:&-cell
       {:width :10em
        :display :inline-block
        :margin-right :1em}]
    ]])

(def post-edit-form
   [:.admin-edit
     [:&-header
       {:height :30vh
        :min-height :300px
        :position :relative
        :overflow :hidden}
       [:&-cover
         {:width :100%
          :object-fit :cover}]
       [:&-white
         {:position :absolute
          :top 0, :bottom 0, :left 0, :right 0
          :background "hsla(0, 0%, 100%, .5)"}]
       [:&-title
         {:position :absolute
          :font "300 4em/1.5 Helvetica, Arial, sans"
          :bottom 0}]]
    ])

(def submitter
  [:.submitter
    {:position :fixed
     :background :white
     :bottom 0
     :width :100%
     :max-width dim-page-max-width
     :padding "1em 0 .5em"
     :border-top "1px solid hsl(0, 0%, 70%)"}
    [:a {:margin-left :3em}]
    [:&-status {:margin-left :3em}]
    [:&-delete
      {:color :red
       :cursor :pointer
       :float :right}]])

(def line
  [:.line
    {:margin-top :1.5em}
    [:&-label
      {:margin-right :1.5em
      :font-size :1.2em}]
    ["textarea, [type=text]"
      {:display :block}]
    [:&-check
      {:margin-right ".4em !important"}]])

(def margins
  (for [i (range 1 101)]
    (list
      [(str ".mt-" i) {:margin-top (str i "px")}]
      [(str ".ml-" i) {:margin-left (str i "px")}]
      [(str ".mr-" i) {:margin-right (str i "px")}]
      [(str ".mb-" i) {:margin-bottom (str i "px")}])))

(def root-panel
  [ margins])

(def root
  [mini-reset
   margins
   [:.html-editor
     {:border "1px solid hsl(0, 0%, 40%)"
      :padding "0.5em"
      :border-radius :4px
      :min-height "5em"}
     [:&--content
       {:min-height "10em"}]]
   [:.page
     {:width :100%
      :max-width dim-page-max-width
      :margin :auto
      :padding-bottom :20vh}]
   [:.page--login
     {:text-align :center
      :padding "30vh 0"}
     [:.form
       {:text-align :left
        :display :inline-block}]]
   post-list
   post-edit-form
   submitter
   [:#uploader
     {:position :absolute
      :left "-200vw"}]
   line
   ])
