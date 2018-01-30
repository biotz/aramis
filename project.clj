(defproject aramis "0.1.1"
  :description "A library providing a Promise.all()-like capabilities for re-frame."
  :url "https://github.com/magnetcoop/aramis"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [re-frame "0.8.0"]]

  :profiles {:debug {:debug true}
             :dev {:plugins [[lein-cljsbuild "1.1.4"]]}}

  :cljsbuild {:builds [{:id           "test"
                        :source-paths ["test" "src"]
                        :compiler     {:preloads      [devtools.preload]
                                       :output-to     "run/compiled/browser/test.js"
                                       :source-map    true
                                       :output-dir    "run/compiled/browser/test"
                                       :optimizations :none
                                       :source-map-timestamp true
                                       :pretty-print  true}}]})
