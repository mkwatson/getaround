(ns getaround.core
  (:require [clj-http.client :as http]))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(def properties ["car_id"
                 "distance"
                 "latitude"
                 "longitude"
                 "make"
                 "model"
                 "price_daily"
                 "price_hourly"
                 "price_weekly"
                 "total_price"
                 "year"
                 "dedicated_parking"])

(def results
  (http/get "https://index.getaround.com/v1.0/search"
            {:query-params {"product" "web"
                            "uid" "100004478731186"
                            "user_lat" "37.7717185"
                            "user_lng" "-122.44389289999998"
                            "lat" "37.7717185"
                            "lng" "-122.44389289999998"
                            "distance" "1"
                            "properties" properties
                            "sort" "best"
                            "page_sort" "magic"
                            "page_size" "500"}}))
