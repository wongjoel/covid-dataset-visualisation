(ns covis.cli
  (:require
   [cli-matic.core :as cli]
   [covis.downloader :as downloader]
   [covis.vis :as vis])
  (:gen-class))

(defn plot-operation
  #_(plot-operation {:out-path "covid_visualisation.html"})
  [{:keys [out-path] :as opts}]
  (let [config {:api-url "https://data.nsw.gov.au/data/api/3/action/datastore_search_sql"
                :dataset "21304414-1ff1-4243-a5d2-f52778048b29"}
        lhds (downloader/get-lhd-names! config)
        formatted-lhds (map (fn [lhd] (if (some? lhd) lhd "Not Listed")) lhds)
        chart-data (downloader/get-counts-by-lhd-and-date! config lhds)
        hiccup (vis/main-spec (vis/chart-spec chart-data formatted-lhds))
        ]
    (vis/export! hiccup out-path)))

(def CONFIGURATION
  {:command "covis"
   :description "Generates COVID-19 Visualisations"
   :version "2021.08.25"
   :subcommands [{:command "plot"
                  :runs plot-operation
                  :opts [{:option "out-path" :as "The output path" :type :string :default "covid_visualisation.html"}]}
                ]})

(defn -main
  "CLI Entry point"
  [& args]
  #_(println args)
  (cli/run-cmd args CONFIGURATION))
