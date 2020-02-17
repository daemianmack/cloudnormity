(ns cloudnormity.impl
  (:require [cloudnormity.tx-sources :as tx-sources]
            [clojure.spec.alpha :as s]
            [datomic.client.api :as d]
            [cloudnormity.specs :as specs]
            [cloudnormity.util :as u]))


(defn tx!
  [conn tx-data]
  (d/transact conn {:tx-data tx-data}))

(defn has-attr?
  [conn attr-name]
  (-> (d/pull (d/db conn) '[:db/id] [:db/ident attr-name])
      :db/id
      boolean))

(def norm-q
  '[:find ?e
    :in $ ?tracking-attr ?norm-name
    :where [?e ?tracking-attr ?norm-name]])

(defn has-norm?
  [conn tracking-attr norm-map]
  (-> (d/q norm-q
           (d/db conn)
           tracking-attr
           (:name norm-map))
      seq
      boolean))

(defn ensure-cloudnormity-schema
  [conn tracking-attr]
  (when-not (has-attr? conn tracking-attr)
    (let [tx-data [{:db/ident tracking-attr
                    :db/valueType :db.type/keyword
                    :db/cardinality :db.cardinality/one
                    :db/doc "Conformed norm name"}]]
      (tx! conn tx-data))))

(defn transact-norm
  "Record having evaluated this norm and include any corresponding
  transaction data."
  [conn norm-map tracking-attr]
  (tx! conn (into [{tracking-attr (:name norm-map)}]
                  (tx-sources/tx-data-for-norm conn norm-map))))

(defn needed?
  [conn norm-map tracking-attr]
  (or (:mutable norm-map)
      (not (has-norm? conn tracking-attr norm-map))))

(defn conform!
  [norm-maps]
  (if-not (s/valid? ::specs/norm-maps norm-maps)
    (u/anomaly! :incorrect
                "Norm config failed to validate."
                {:problems (s/explain ::specs/norm-maps norm-maps)})
    (s/conform ::specs/norm-maps norm-maps)))