(ns cloudnormity.tx-sources
  (:require [cloudnormity.util :as u]))


(defn dispatch
  [conn norm-map]
  (-> norm-map :tx-source first))

(defmulti tx-data-for-norm dispatch)

(defmethod tx-data-for-norm :tx-data tx-data
  [_ norm-map]
  (-> norm-map :tx-source second))

(defmethod tx-data-for-norm :tx-resource tx-resource
  [_ norm-map]
  (-> norm-map :tx-source second u/read-resource))

(defn eval-tx-fn
  [conn tx-fn]
  (try ((requiring-resolve tx-fn) conn)
       (catch Throwable t
         (throw (ex-info (str "Exception evaluating " tx-fn)
                         {:exception t})))))

(defmethod tx-data-for-norm :tx-fn tx-fn
  [conn norm-map]
  (->> norm-map :tx-source second (eval-tx-fn conn)))


(defn reorg-tx-sources
  "If a key in the `norm-map` has a method implementation in
  `tx-data-for-norm`, relocate it underneath a `:tx-source` key with a
  vector conveying its type and value.

  This allows the user's config to reference novel tx sources while
  keeping config syntax as simple and terse as possible (i.e., user
  does not have to provide an extraneous `type` key, or preserve
  special structure) yet also gives `tx-data-for-norm` a dispatchable
  value at a predictable coordinate."
  [norm-map]
  (let [meths (methods tx-data-for-norm)]
    (reduce-kv
     (fn [acc k v]
       (if (contains? meths k)
         (reduced (-> acc
                      (assoc :tx-source [k v])
                      (dissoc k)))
         acc))
     norm-map
     norm-map)))