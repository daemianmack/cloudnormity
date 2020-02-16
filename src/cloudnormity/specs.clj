(ns cloudnormity.specs
  (:require [clojure.spec.alpha :as s]
            [cloudnormity.tx-sources :as tx-sources]
            [cloudnormity.util :as u]))


(s/def ::tx-data (s/coll-of map?))
(s/def ::tx-fn symbol?)
(s/def ::tx-resource string?)

(s/def ::mutable boolean?)
(s/def ::name keyword?)

(s/def ::norm-map
  (s/and (s/keys :req-un [::name]
                 :opt-un [::tx-data
                          ::tx-resource
                          ::tx-fn
                          ::mutable])
         (s/conformer tx-sources/reorg-tx-sources)))

(s/def ::norm-maps
  (s/coll-of ::norm-map))

(defn conform!
  [norm-maps]
  (if-not (s/valid? ::norm-maps norm-maps)
    (u/anomaly! :incorrect
                "Norm config failed to validate."
                {:problems (s/explain ::norm-maps norm-maps)})
    (s/conform ::norm-maps norm-maps)))