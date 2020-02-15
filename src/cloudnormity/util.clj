(ns cloudnormity.util
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))


(defn anom-map
  [category msg]
  {:cognitect.anomalies/category (keyword "cognitect.anomalies" (name category))
   :cognitect.anomalies/message msg})

(defn anomaly!
  ([name msg]
   (throw (ex-info msg (anom-map name msg))))
  ([name msg extra]
   (throw (ex-info msg (merge (anom-map name msg) extra)))))

(defn read-resource
  "Reads and returns data from a resource containing edn text."
  [resource-name]
  (with-open [reader (->> (io/resource resource-name)
                          io/reader
                          (java.io.PushbackReader.))]
    (edn/read reader)))
