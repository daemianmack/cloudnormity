(ns cloudnormity.api
  (:require [cloudnormity.impl :as impl]))


(def ^:dynamic *tracking-attr* :cloudnormity/conformed)


(defn ensure-norms
  [conn norm-maps]
  (reduce
   (fn [acc {name :name :as norm-map}]
     (if (not (impl/needed? conn norm-map *tracking-attr*))
       acc
       (try
         (impl/transact-norm conn norm-map *tracking-attr*)
         (conj acc name)
         (catch Exception e
           (throw (ex-info "Norm failed to conform"
                           {:succeeded-norms acc
                            :failed-norm name
                            :exception e}))))))
   []
   norm-maps))

(defn norm-maps-by-name
  [norm-maps names]
  (filter (comp (set names) :name)
          norm-maps))

(defn ensure-conforms
  ([conn norm-maps]
   (ensure-conforms conn norm-maps (map :name norm-maps)))
  ([conn norm-maps norm-names]
   (impl/ensure-cloudnormity-schema conn *tracking-attr*)
   (let [ensurable-norms (norm-maps-by-name norm-maps norm-names)]
     (ensure-norms conn ensurable-norms))))