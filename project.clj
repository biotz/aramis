(defproject aramis "0.1.1"
  :description "A library providing a Promise.all()-like capabilities for re-frame."
  :url "https://github.com/magnetcoop/aramis"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[lein-doo "0.1.8"]
                 [devcards "0.2.4"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [re-frame "0.8.0"]
                 [day8.re-frame/test "0.1.5"]]
  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-doo "0.1.8"]
            [lein-figwheel "0.5.14"]]
  :cljsbuild {:test-commands {"test" ["lein" "doo" "phantom" "test" "once"]}
              :builds [{:id "test"
                        :source-paths ["test" "src"]
                        :compiler
                        {:main runners.doo
                         :optimizations :none
                         :output-to "resources/public/cljs/tests/all-tests.js"}}
                       {:id "devcards-test"
                        :source-paths ["src" "test"]
                        :figwheel {:devcards true}
                        :compiler {:main runners.browser
                                   :optimizations :none
                                    :asset-path "cljs/tests/out"
                                   :output-dir "resources/public/cljs/tests/out"
                                   :output-to "resources/public/cljs/tests/all-tests.js"
                                   :source-map-timestamp true}}]})
