(ns cloudnormity.specs
  (:require [clojure.spec.alpha :as s]
            [cloudnormity.tx-sources :as tx-sources]))


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