(ns getaround.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.core.reducers :as r]
            [clojure.string :as s]))

;; TODO: Use core.async instead of clojure.core.reducers

(def join-query-arr (partial s/join ","))

(def properties
  (join-query-arr ["car_id"
                   "car_name"
                   "latitude"
                   "longitude"
                   "make"
                   "model"
                   "price_daily"
                   "price_hourly"
                   "price_weekly"
                   "total_price"
                   "year"
                   "dedicated_parking"]))

(def viewport
  (join-query-arr [37.693589 -122.535283 37.810043 -122.343709]))

(def clj-keywords (comp keyword #(s/replace % "_" "-")))

(defn sf-cars []
  (let [raw-results (http/get "https://index.getaround.com/v1.0/search"
                              {:query-params {"viewport" viewport
                                              "properties" properties
                                              ;; "page_size" 1
                                              }})]
    (-> raw-results
        :body
        (json/parse-string clj-keywords)
        :cars)))


;; TODO: Do we filter only on days in the future? Probably
(defn filter-dates
  [dates]
  (filter dates))

;; TODO: trim regex to only match availability
;; so that we don't have to json-parse the entire document
(defn availability
  [car-name]
  (let [raw-results (-> (http/get (str "https://www.getaround.com/" car-name))
                        :body)]
    (-> (re-find #"(?s)(?<=window.GA_ARGS = ).*?(?=</script>)" raw-results)
        (json/parse-string clj-keywords)
        :car
        :availability
        ;; json/generate-string
        ;; filter-dates
        )))

(defn add-availability
  [{:keys [car-name] :as car}]
  (assoc car :availability (availability car-name)))

(defn write-to-file
  [data]
  (json/generate-stream data (clojure.java.io/writer "resources/sfcars.json")))

(defn results []
  (->> (r/map add-availability (sf-cars))
       (into [])
       write-to-file))

;; TODO
;; Every 17.3 seconds, use edmunds API to look up the price of a car
;; First make up at least one (median age) of each make.
;; Then one of each make/model. Then fill out all values.
;; We need the milage of the car, so let's use 12,000 miles per year
;; eg A model year 2015 car would be 2 years old, and have 24,000 miles on it
;; Also, need to lookup the zip from the lat-long.

(def makes
  (->> (json/parse-string (slurp "resources/sfcars.json") clj-keywords)
       (map :make)
       set))

;; (count makes) => 34

(def edmunds-api "")

;; TODO: Kind of sucks parsing the string, just to write it to disk
;; Let's string manipulate that shit and shove the make in there
(defn get-models
  [make]
  (let [raw-results (http/get (str "https://api.edmunds.com/api/vehicle/v2/" make "/models")
                              {:query-params {"fmt" "json"
                                              "api_key" edmunds-api}})]
    (-> raw-results
        :body
        (json/parse-string clj-keywords)
        (assoc :make make))))

(defn persist-models
  [{:keys [make] :as models}]
  (json/generate-stream models (clojure.java.io/writer (str "models/" make ".json"))))

(defn get-all-models
  []
  (doseq [car (->> cars
                   (map :make)
                   set)]
    (-> car
        get-models
        persist-models)))
