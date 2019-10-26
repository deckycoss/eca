(ns eca.core
  (:require [reagent.core :as reagent :refer [atom]]))

(defonce num-rows (atom 16))
(defonce num-cols (atom 32))
(defonce rule (atom 1))
(defonce grid-data (-> (map bit-set
                            (repeat 0)
                            (repeatedly @num-rows #(rand-int @num-cols)))
                       vec
                       atom))
(defonce iterate-down? (atom false))
(defonce cell-size (atom 20))
(defonce autoplay? (atom false))

(defn cell [{:keys [size row-index col-index state]}]
  (let [attrs {:width size
               :height size
               :x (* col-index size)
               :y (* row-index size)
               :fill (if-not state "none")}]
    [:rect attrs]))

(defn row [{:keys [row-data cell-size row-index row-length]}]
  (into [:g]
          (map-indexed
           #(vector cell {:size cell-size
                          :row-index row-index
                          :col-index %2
                          :state (bit-test row-data %1) })
           (range row-length))))

(defn grid [{:keys [data cell-size row-length]}]
  (into [:g]
          (map-indexed
           #(vector row {:row-length row-length
                         :cell-size cell-size
                         :row-index %1
                         :row-data %2})
           data)))

(defn bit-value [x i]
  (if (bit-test x i) 1 0))

(defn flip-bit [coll, row-index, col-index]
  (update coll
          row-index
          bit-flip
          col-index))

(defn nth-bit-triad
  ([x full-length i big-endian?]
   (map
    #(bit-value x (mod (+ % i) full-length))
    (if big-endian?
        (range 1 -2 -1)
        (range -1 2 1))))
  ([x full-length i]
   (nth-bit-triad x full-length i false)))

(defn bit-triads [x full-length]
  (map #(nth-bit-triad x full-length %) (range full-length)))

(defn bits-to-int [bits]
  (->> bits
       (map-indexed #(bit-shift-left %2 %1))
       (reduce +)))

(defn apply-rule [x rule length]
  (bits-to-int
    (map #(bit-value rule (bits-to-int %)) (bit-triads x length))))

(defn on-svg-click [e]
  (let [bound (.. e -target getBoundingClientRect)
        s @cell-size
        x (- (. e -clientX) (. bound -left))
        y (- (. e -clientY) (. bound -top))
        col-index (. js/Math floor (/ x s))
        row-index (. js/Math floor (/ y s))]
    (swap! grid-data flip-bit row-index col-index 0)))
    ;(println (. js/Math floor (/ x s)))))

(defn advance-grid []
  (swap! grid-data
         #(if @iterate-down?
              (do (conj
               (subvec % 1)
               (apply-rule (peek %) @rule @num-cols)))
              (vec (for [x %]
               (apply-rule x @rule @num-cols))))))

(defn on-iterate-checkbox-change [e]
  (reset! iterate-down? (.. e -target -checked)))

(defn app []
  [:div
   [:div#svg-container
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :fill "black"
           :width "100%"
           :height "100%"
           :stroke "none"
           :pointerEvents "all"
           :shape-rendering "crispEdges"}
     [grid {:data @grid-data
            :cell-size @cell-size
            :row-length @num-cols}]
     [:rect {:fill "none"
             :onClick on-svg-click
             ;:onMouseOver on-cell-mouse-over
             :width (* @cell-size @num-cols)
             :height (* @cell-size @num-rows)}]]
    [:button {:onClick advance-grid} "Step"]
    [:label
     "Rule"
     [:input {:type "number"
              :value @rule
              :onChange #(reset! rule (.. % -target -value))
              :min 0
              :max 255}]]
    [:label
     "Iterate downward"
     [:input {:type "checkbox"
              :onChange on-iterate-checkbox-change}]]]
   ])

(defn start []
  (reagent/render-component [app]
                            (. js/document (getElementById "app"))))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))
