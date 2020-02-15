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
