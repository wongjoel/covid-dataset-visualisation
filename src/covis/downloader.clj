(ns covis.downloader
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  (:import (java.time LocalDate LocalDateTime ZonedDateTime ZoneId)
           (java.time.format DateTimeFormatter)))

(defn gen-date-seq
  #_(gen-date-seq (LocalDate/parse "2020-01-01") (LocalDate/parse "2020-02-01"))
  "Generated the sequence of dates from the start date (inclusive) to the end date (exclusive)"
  [start-date end-date]
  (iterator-seq (.iterator (.datesUntil start-date end-date))))

(defn get-dataset-update-from-metadata!
  #_(get-dataset-update-from-metadata! config)
  [{:keys [metadata-url]}]
  (let [response (client/get metadata-url)
        body (json/read-str (:body response))
        datetime-str (get (first (get-in body ["result" "resources"])) "last_modified")
        formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss.nX")
        datetime (.withZoneSameInstant (ZonedDateTime/parse (str datetime-str "Z") formatter) (ZoneId/systemDefault))]
    (.toLocalDateTime datetime)))

(defn simple-sql-get!
  [url sql]
  (client/get url {:query-params {:sql sql}}))

(defn make-sql-star-unsafe
  #_(make-sql-star-unsafe "abc")
  [dataset]
  (str "SELECT * FROM \"" dataset "\""))

(defn make-sql-distinct-unsafe
  #_(make-sql-distinct-unsafe "abc")
  [dataset]
  (str "SELECT DISTINCT lhd_2010_name FROM \"" dataset "\""))

(defn make-sql-count-lhd-unsafe
  #_(make-sql-count-lhd-unsafe "abc" "2020" "def")
  [dataset date lhd]
  (str "SELECT COUNT(notification_date) FROM \"" dataset "\" WHERE notification_date='" date "' AND lhd_2010_name='" lhd "'"))

(defn make-sql-count-null-unsafe
  #_(make-sql-count-null-unsafe "abc" "2020")
  [dataset date]
  (str "SELECT COUNT(notification_date) FROM \"" dataset "\" WHERE notification_date='" date "' AND lhd_2010_name is NULL"))

(defn make-sql-count-total-unsafe
  #_(make-sql-count-total-unsafe "abc" "2020")
  [dataset date]
  (str "SELECT COUNT(notification_date) FROM \"" dataset "\" WHERE notification_date='" date "'"))

(defn get-lhd-names!
  #_(get-lhd-names config)
  [{:keys [api-url dataset] :as config}]
  (let [response (simple-sql-get! api-url (make-sql-distinct-unsafe dataset))
        body (json/read-str (:body response))
        records (get-in body ["result" "records"])]
    (->> records
         (map (fn [record] (get record "lhd_2010_name"))))))

(defn get-total-count-for-date!
  #_(get-total-count-for-date! config "2021-08-20")
  [{:keys [api-url dataset]} date]
    (let [response (simple-sql-get! api-url (make-sql-count-total-unsafe dataset date))
        body (json/read-str (:body response))
        records (get-in body ["result" "records"])]
    (get (first records) "count")))

(defn get-null-count-for-date!
  #_(get-null-count-for-date! config "2021-08-20")
  [{:keys [api-url dataset]} date]
  (let [response (simple-sql-get! api-url (make-sql-count-null-unsafe dataset date))
        body (json/read-str (:body response))
        records (get-in body ["result" "records"])]
    (get (first records) "count")))

(defn get-lhd-count-for-date!
  #_(get-lhd-count-for-date! config "2021-08-20" "Hunter New England")
  [{:keys [api-url dataset]} date lhd]
  (let [response (simple-sql-get! api-url (make-sql-count-lhd-unsafe dataset date lhd))
        body (json/read-str (:body response))
        records (get-in body ["result" "records"])]
    (get (first records) "count")))

(defn get-region-count-for-date!
  #_(get-region-count-for-date! config "2021-08-20" "Hunter New England")
  #_(get-region-count-for-date! config "2021-08-20" nil)
  [config date lhd]
  (if (some? lhd)
    (get-lhd-count-for-date! config date lhd)
    (get-null-count-for-date! config date)))

(defn get-counts-by-lhd-and-date!
  #_(get-counts-by-lhd-and-date! config (get-lhd-names! config) "2021-08-25")
  [config lhd-names last-update]
  (->> (for [lhd lhd-names
             date (gen-date-seq (.minusDays (.toLocalDate last-update) 15) (.toLocalDate last-update))]
         {:lhd lhd
          :date date})
       (map (fn [{:keys [date lhd] :as run}] (assoc run :count (get-region-count-for-date! config date lhd))))
       (map (fn [{:keys [date lhd count]}] {:count count
                                            :notification-date (str date)
                                            :health-district (if (some? lhd) lhd "Not Listed")}))))



(comment
  (def dataset1 "21304414-1ff1-4243-a5d2-f52778048b29")
  (def dataset2 "97ea2424-abaf-4f3e-a9f2-b5c883f42b6a")
  (def api-url "https://data.nsw.gov.au/data/api/3/action/datastore_search_sql")

  (def config
    {:api-url "https://data.nsw.gov.au/data/api/3/action/datastore_search_sql"
     :metadata-url "https://data.nsw.gov.au/data/api/3/action/package_show?id=aefcde60-3b0c-4bc0-9af1-6fe652944ec2"
     :dataset "21304414-1ff1-4243-a5d2-f52778048b29"})

  (def response1
    (client/get (:metadata-url config)))

  (def response2
    (client/get "https://data.nsw.gov.au/data/api/3/action/datastore_search" {:query-params {:resource_id "2776dbb8-f807-4fb2-b1ed-184a6fc2c8aa"}}))

  (def response3
  (client/get "https://data.nsw.gov.au/data/api/3/action/datastore_search_sql" {:query-params {:sql "SELECT COUNT(notification_date) FROM \"2776dbb8-f807-4fb2-b1ed-184a6fc2c8aa\" WHERE notification_date='2021-08-20'"}}))
  )
