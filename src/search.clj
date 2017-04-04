(ns search
  (:require [incanter.stats :refer [levenshtein-distance]]
            [cheshire.core :as json]
            [clojure.string :refer [lower-case] :as s]))

(def edmunds-cars
  (->> (json/parse-stream (clojure.java.io/reader "edmunds-cars/models/all.json") true)
                         (map (fn [{:keys [make models]}]
                                [(lower-case make) (set (map :niceName models))]))
                         (into {})))

(def getaround-cars
  (->> (json/parse-stream (clojure.java.io/reader "output/text.json") true)
       :cars
       (map #(select-keys % [:make :model]))
       (group-by :make)
       (map (fn [[make models]] [(lower-case make)
                                 (set (map :model models))]))
       (into {})))

(def makes (comp set keys))

(def edmunds-makes (makes edmunds-cars))
(def getaround-makes (makes getaround-cars))

;; (clojure.set/superset? edmunds-makes getaround-makes)
;; => true

(def normalize-getaround-model
  (comp (comp #(s/replace % " " "-") lower-case)))

(def distances (atom '()))

(defn find-closest
  [model models]
  (let [distance-models (->> models
                             (map (fn [m] [m (levenshtein-distance (normalize-getaround-model model) m)]))
                             (group-by second)
                             (sort-by key)
                             first)
        matches (-> distance-models
                    val
                    (map first))
        distance (key distance-models)
        result (first matches)]
    (set! distances (partial conj distance))
    (if (> (count matches) 1)
      (throw (Exception. (str model "has mutliple matches:" matches)))
      result)))

(defn get-normalized-models
  [subset superset]
  (let [normalize-models (fn [[make models]]
                           [make (->> models
                                      (map (fn [model] {model (find-closest model (superset make))})))])]
    (->> subset
         (map normalize-models)
         (into {}))))

;; (json/generate-stream (get-normalized-models getaround-cars edmunds-cars)
;;                       (clojure.java.io/writer "normalized-models.json"))

;; Looks like audi a3 is wrong
;; Not sure what mazda-3 is
