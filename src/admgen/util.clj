(ns admgen.util
  (:import java.net.URLEncoder))

(defn- urlenc [str]
  (URLEncoder/encode str))

(defn form-encode [m]
  (clojure.string/join "&"
    (for [[k v] m]
      (str
        (urlenc (name k))
        "="
        (urlenc (str v))))))
