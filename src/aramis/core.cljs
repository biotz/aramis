(ns aramis.core
  (:require [re-frame.core :as rf]))

;;;; FIXME put title and description of what it does

;;; FIXME Put that note in correct place
;;; Events registered through need aramis need to have, as a first argument, a hashmap containing at least `:event-id`.
;;; Example: [::foo/fetch {:event-id :buying-apples :quantity 2} more-args]

;;; Example data-structure of `(:aramis db)`:

;; {:group-1-id
;;  {:pending #{:event-1-id :event-2-id}
;;   :done #{:event-3-id}
;;   :once-done [::foo/say-hello "World"]}
;;  :group-2-id
;;  {:pending #{:event-2-id :event-99-id}
;;   :once-done [::foo/say-hello "Group 2"]}}

;;; STATUS

(rf/reg-sub
  ::aramis
  (fn [db _]
    (:aramis db)))

(rf/reg-sub
  ::group-status                                            ; usage: (subscribe [::group-status :group1-id])
  (fn [_ _]
    (rf/subscribe [::aramis]))

  ;; Returns a hashmap containing `:pending`, `:done` and `:once-done` of a group
  (fn [aramis [_ group-id]]
    (get aramis group-id)))

(rf/reg-sub
  ::event-info                                              ; usage: (subscribe [::event-info :event-x-id])
  (fn [_ _]
    (rf/subscribe [::aramis]))

  ;; Returns a hashmap containing:
  ;;  :status - status of an event being either `:pending`, `:done` (or `nil` if event-id is not registered)
  ;;           Assumes that an event has the same status in every group it collaborates to.
  ;;  :collaborates-to - a set of groups ids where event has a status
  (fn [aramis [_ event-id]]
    (let [group-status-pairs (remove nil?
                                     (mapv
                                       (fn [[group-id {:keys [pending done]}]]
                                         (cond
                                           (get pending event-id) [group-id :pending]
                                           (get done event-id) [group-id :done]))
                                       aramis))]
      {:status (some-> group-status-pairs first second)
       :collaborates-to (set (map first group-status-pairs))})))

(rf/reg-sub
  ::event-status                                            ; usage: (subscribe [::event-status :event-x-id])
  (fn [[_ event-id] _]
    (rf/subscribe [::event-info event-id]))
  (fn [{:keys [status] :as info} _]
    status))

(rf/reg-sub
  ::event-status-in-group                                   ; usage: (subscribe [::event-status-in-group :group-1-id :event-x-id])
  (fn [[_ group-id _] _]
    (rf/subscribe [::group-status group-id]))
  (fn [{:keys [pending done]} [_ _ event-id]]

    ;; Returns a status of an event in group given by group-id
    (cond
      (get pending event-id) :pending
      (get done event-id) :done)))

;;; OPENING - creating groups and new collaborators

(rf/reg-event-db
  ::add-collaborator                                        ; usage: (dispatch [::add-collaborator :g-1-id :e-x-id])
  (fn [db [_ group-id event-id]]
    (update-in db [:aramis group-id :pending]
               (fnil #(conj % event-id) #{}))))

(rf/reg-event-fx
  ;; usage: (dispatch [::one-of :g-1-id [::foo/fetch {:event-id :e-x-id}]])
  ::one-of

  ;; Wrapper for a dispatch being one of the collaborators to group.
  ;; Apart from the actual dispatch it also dispatches that collaborator registration.
  (fn [{:keys [db]} [_ group-id event]]
    (let [event-id (get-in event [1 :event-id])]
      {:dispatch-n [[::add-collaborator group-id event-id]
                    event]})))

(rf/reg-event-db
  ;; usage: (dispatch [::once-all-done :g-1-id [::foo/say-hello "World"]])
  ::once-all-done
  (fn [db [_ group-id once-done]]

    ;; Stores an information about an event to be ran once all collaborators to group are done.
    (assoc-in db [:aramis group-id :once-done] once-done)))

(rf/reg-event-fx
  ;; usage: (dispatch [::wait-for-all-to
  ;;                   [[::foo/fetch {:event-id :buying-tomatoes}]
  ;;                    [::foo/fetch {:event-id :buying-cabbage}]]
  ;;                   [::foo/make-salad]])
  ;; optionally, you can explicitly set `:group-id`
  ::wait-for-all-to

  ;; Wraps each of the collaborator in `::one-of` event.
  ;; Then dispatches those wraps alongside with `:once-done` event.
  (fn [_ [_ dispatches once-all-done & {:keys [group-id]
                                        :or {group-id (random-uuid)}}]]
    {:dispatch-n (conj
                   (mapv
                     (fn [dispatch]
                       [::one-of group-id dispatch]) dispatches)
                   [::once-all-done group-id once-all-done])}))

;; CLOSURE - collaborators reporting and groups completion

(rf/reg-event-db
  ::complete-collaborator
  (fn [db [_ group-id event-id]]
    (-> db
        (update-in [:aramis group-id :pending] disj event-id)
        (update-in [:aramis group-id :done] (fnil #(conj % event-id) #{})))))

(rf/reg-event-fx
  ::report-to-group
  (fn [{:keys [db]} [_ group-id event-id]]
    {:dispatch (if (= (get-in db [:aramis group-id :pending])
                      #{event-id})
                 [::complete group-id]
                 [::complete-collaborator group-id event-id])}))

(rf/reg-event-fx
  ;; This needs to be dispatched in `success` handler of the collaborator.
  ;; usage: (dispatch [::report :event-x-id])
  ::report

  ;; Finds every group event collaborates to and reports to each one of them.
  (fn [{:keys [db]} [_ event-id]]
    {:dispatch-n
     (remove nil?
             (mapv
               (fn [[group-id {:keys [pending]}]]
                 (if (get pending event-id)
                   [::report-to-group group-id event-id]))
               (:aramis db)))}))

(rf/reg-event-fx
  ;; Once all collaborators to a group are marked `:done`, then that group is removed from `:aramis` register.
  ::complete
  (fn [{:keys [db]} [_ group-id]]
    {:dispatch (get-in db [:aramis group-id :once-done])
     :db (update db :aramis dissoc group-id)}))