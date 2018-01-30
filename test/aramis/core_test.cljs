(ns aramis.core-test
  (:require [cljs.test :refer-macros [is testing]]
            [devcards.core :refer-macros [deftest]]
            [re-frame.core :as rf]
            [re-frame.db :as db]
            [day8.re-frame.test :as rf-test]
            [aramis.core :as aramis]))

(rf/reg-event-db
  ::foo
  (fn [db _]
    (prn "This group just finished its work")
    db))

(deftest
  group-status
  (reset! db/app-db {:aramis {}})
  (let [group-id (random-uuid)
        group-status (rf/subscribe [::aramis/group-status group-id])]
    (testing "group status doesn't exist before it's created"
      (is (nil? @group-status)))
    (testing "group created by giving `once-done` event"
      (rf/dispatch-sync [::aramis/once-all-done group-id [::foo]])
      (is (some? @group-status)))
    (testing ":pending set is initially empty"
      (is (empty? (:pending @group-status))))
    (let [event-1-id (random-uuid)
          event-2-id (random-uuid)]
      (testing "Adding collaborator to group"
        (rf/dispatch-sync [::aramis/add-collaborator group-id event-1-id])
        (is (some? (:pending @group-status))))
      (testing "Completing a collaborator in a group"
        (rf/dispatch-sync [::aramis/complete-collaborator group-id event-1-id])
        (is (empty? (:pending @group-status)))
        (is (= #{event-1-id} (:done @group-status))))
      (testing "Adding one more"
        (rf/dispatch-sync [::aramis/add-collaborator group-id event-2-id])
        (is (= #{event-2-id} (:pending @group-status)))
        (is (= #{event-1-id} (:done @group-status))))
      (testing "Completing second collaborator"
        (rf/dispatch-sync [::aramis/complete-collaborator group-id event-2-id])
        (is (= #{event-1-id event-2-id} (:done @group-status)))
        (is (empty? (:pending @group-status))))
      (testing "Completing a group"
        (rf/dispatch-sync [::aramis/complete group-id])
        (is (nil? @group-status))))))