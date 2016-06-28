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

