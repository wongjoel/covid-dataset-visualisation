(ns covis.vis
  "Working space for covid dataset"
  (:require
   [oz.core :as oz])
  (:import
   (java.time LocalDate LocalDateTime)
   (java.time.format DateTimeFormatter)))

(defn chart-spec
  [chart-data region-seq partial-date]
  [:vega-lite
     {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
      :data {:values chart-data}
      :title "Case count by Notification Date"
      :width 1100
      :height 700
      :mark {:type "bar" :tooltip true}
      :params [{:name "user_selected"
                :select {:type "point" :fields ["health-district"]}
                :bind {:input "select" :options (concat [nil] region-seq)}}]
      :transform [{:filter {:param "user_selected"}}]
      :encoding {:x {:field "notification-date"
                     :type "ordinal"
                     :axis {:title "Notification Date"}}
                 :y {:field "count"
                     :type "quantitative"
                     :axis {:title "Case Count"}}
                 :color {:field "health-district"
                         :scale {:scheme "category20"}
                         :type "nominal"}
                 :opacity {:condition {:test {:field "notification-date"
                                              :gte partial-date}
                                       :value 0.6}
                           :value 1}}}])

(defn main-spec
  [chart-spec last-update]
  [:section
   [:h1 "NSW Covid-19 cases by notification date and location"]
   chart-spec
   [:p "Chart generated at: " (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss") (LocalDateTime/now)) ", underlying data last updated at " (.format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss") last-update)]
   [:p "This is a visualisation of the "
    [:a {:href "https://data.nsw.gov.au/data/dataset/aefcde60-3b0c-4bc0-9af1-6fe652944ec2"} "NSW Covid-19 cases by notification date and location"]
    " dataset, which is part of the "
    [:a {:href "https://data.nsw.gov.au/nsw-covid-19-data/cases"} "NSW COVID-19 cases data"]
    " made available as part of "
    [:a {:href "https://data.nsw.gov.au/datansw"} "Data.NSW"]
    ". Filter by local health district (lhd_2010_name) by using the drop-down menu."]
   [:p " Some notes:"]
   [:ul
    [:li "These numbers do not match exactly with the number of new infections reported in the media (Likely due to a difference between midnight to midnight vs 8pm to 8pm reporting)"]
    [:li "The most recent day is a partial day, only containing data up to 8pm, and is semi-transparent to reflect this"]
    [:li "The underlying dataset is updated at ~2:30pm on weekdays, with data from up to ~8pm of the previous day"]
    [:li "Cases without a specified local health district are placed in the \"Not Listed\" category"]
    [:li "It is assumed that each row of the dataset corresponds to a single case"]
    [:li "As noted in the original dataset, the reported location is based on the usual residence of the case, not the location of infection."]
    [:li "As noted in the original dataset, some cases, such as crews of ships docked in NSW are not part of this dataset"]
    [:li "Only the previous 14 days are displayed to keep the visualisation readable"]
    ]
   [:p "The visualisation was produced as follows:"]
   [:ol
    [:li "Data is obtained in CSV format from " [:a {:href "https://data.nsw.gov.au/data/dataset/aefcde60-3b0c-4bc0-9af1-6fe652944ec2"} "Data.NSW's website"]]
    [:li "Unique values for the \"lhd_2010_name\" column are extracted to obtain local heath districts"]
    [:li "The previous 14 days from the generation date are calculated"]
    [:li "For each date, for each local health district, the number of rows containing the given local health district are counted"]
    [:li "The data is then formatted into the above visualisation"]]
   [:p "The code used to produce the visualisation can be found at " [:a {:href "https://github.com/wongjoel/covid-dataset-visualisation"} "https://github.com/wongjoel/covid-dataset-visualisation"]]
   ])

(defn temp-fix-vega-lite-version
  [path]
  (println "temp fix running")
  (spit
   path
   (clojure.string/replace
    (slurp path)
    #"https://cdn.jsdelivr.net/npm/vega-lite@4.17.0"
    "https://cdn.jsdelivr.net/npm/vega-lite@5")))

(defn add-p-newlines
  [path]
  (spit
   path
   (clojure.string/replace
    (slurp path)
    #"</p>"
    "</p>\n")))

(defn add-head-newlines
  [path]
  (spit
   path
   (clojure.string/replace
    (slurp path)
    #"</head>"
    "</head>\n")))

(defn add-div-newlines
  [path]
  (spit
   path
   (clojure.string/replace
    (slurp path)
    #"<div>"
    "\n<div>")))

(defn export!
  #_(export! hiccup "/test.html")
  "Wraps `oz/export!` with some niceties"
  [hiccup filepath]
  (oz/export! hiccup filepath {:header-extras [[:script {:type "text/javascript"} (str "document.title = \"Covid Visualisation\";")]]})
  (temp-fix-vega-lite-version filepath)
  (add-p-newlines filepath)
  (add-head-newlines filepath)
  (add-div-newlines filepath))
