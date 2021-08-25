(ns covis.vis
  "Working space for covid dataset"
  (:require
   [oz.core :as oz])
  (:import
   (java.time LocalDate LocalDateTime)))

(defn chart-spec
  [chart-data region-seq]
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
                         :type "nominal"}}}])

(defn main-spec
  [chart-spec]
  [:section
   [:h1 "NSW Covid-19 cases by notification date and location"]
   chart-spec
   [:p "Chart generated at: " (str (LocalDateTime/now))]
   [:p "This is a visualisation of the "
    [:a {:href "https://data.nsw.gov.au/data/dataset/aefcde60-3b0c-4bc0-9af1-6fe652944ec2"} "NSW Covid-19 cases by notification date and location"]
    " dataset, which is part of the "
    [:a {:href "https://data.nsw.gov.au/nsw-covid-19-data/cases"} "NSW COVID-19 cases data"]
    " made available as part of "
    [:a {:href "https://data.nsw.gov.au/datansw"} "Data.NSW"]
    ". Filter by local health district (lhd_2010_name) by using the drop-down menu."]
   [:p " Some notes:"]
   [:ul
    [:li "These numbers do not match exactly with the number of new infections reported in the media (I haven't worked out the reason for this yet)"]
    [:li "The most recent day tends to have more cases added after the next update (it seems more cases are added in retrospect)"]
    [:li "The underlying dataset is updated at ~2:30pm on weekdays, with data from up to ~8pm of the previous day"]
    [:li "Cases without a specified local health district are placed in the \"Not Listed\" category"]
    [:li "It is assumed that each row of the dataset corresponds to a single case"]
    [:li "As noted in the original dataset, the reported location is based on the usual residence of the case, not the location of infection."]
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

(defn add-newlines
  [path]
  (spit
   path
   (clojure.string/replace
    (slurp path)
    #"</p>"
    "</p>\n")))

(defn export!
  #_(export! hiccup "/test.html")
  "Wraps `oz/export!` with some niceties"
  [hiccup filepath]
  (oz/export! hiccup filepath {:header-extras [[:script {:type "text/javascript"} (str "document.title = \"Covid Visualisation\";")]]})
  (temp-fix-vega-lite-version filepath)
  (add-newlines filepath))
