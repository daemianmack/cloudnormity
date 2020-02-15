(ns cloudnormity.specs
  (:require [clojure.spec.alpha :as s]
            [cloudnormity.tx-sources :as tx-sources]
            [cloudnormity.util :as u]))


(s/def ::tx-data (s/coll-of map?))
(s/def ::tx-fn symbol?)
(s/def ::tx-resource string?)

(s/def ::mutable boolean?)
(s/def ::name keyword?)

(defn distinguish-tx-sources
  "If a key in the `norm-map` has a method implementation in
  `tx-data-for-norm`, relocate it underneath a `:tx-source` key with a
  vector conveying its type and contents.

  This allows the user's config to reference novel tx sources while
  keeping general-usage syntax as simple and terse as possible (i.e.,
  user does not have to provide a `type` key or worry about special
  structure) yet also give `tx-data-for-norm` something at a known
  coordinate to dispatch on."
  [norm-map]
  (let [meths (methods tx-sources/tx-data-for-norm)]
    (reduce-kv
     (fn [acc k v]
       (if (contains? meths k)
         (reduced (-> acc
                      (assoc :tx-source [k v])
                      (dissoc k)))
         acc))
     norm-map
     norm-map)))

(s/def ::norm-map
  (s/and (s/keys :req-un [::name]
                 :opt-un [::tx-data
                          ::tx-resource
                          ::tx-fn
                          ::mutable])
         (s/conformer distinguish-tx-sources)))

(s/def ::norm-maps
  (s/coll-of ::norm-map))

(defn conform!
  [norm-maps]
  (if-not (s/valid? ::norm-maps norm-maps)
    (u/anomaly! :incorrect
                "Norm config failed to validate."
                {:problems (s/explain ::norm-maps norm-maps)})
    (s/conform ::norm-maps norm-maps)))