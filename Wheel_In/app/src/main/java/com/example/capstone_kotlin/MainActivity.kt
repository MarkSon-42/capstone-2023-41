package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davemorrissey.labs.subscaleview.ImageSource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE


class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음

    // 지도
    private lateinit var map: PinView

    // 서치뷰, QR, 취소 버튼 레이아웃
    private lateinit var searchView1: SearchView
    private lateinit var searchView2: SearchView
    private lateinit var searchView_layout: LinearLayout
    private lateinit var cancel: Button
    private lateinit var svAndCancel: LinearLayout


    // 출발지 목적지 같은 값이 들어가지 않게 확인하는 변수
    private var checkS1: String? = null
    private var checkS2: String? = null

    // 정보창 및 표시될 사진, 지명, 접근성
    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView

    private lateinit var spinner: Spinner

    private lateinit var info_elvt: LinearLayout
    private lateinit var detail: TextView

    private lateinit var btn_back: Button
    private lateinit var btn_elvt: Button

    // QR 촬영으로 값 받아올 변수
    private var id: DataBaseHelper.PlaceNode? = null

    // 교차로 터치 이벤트 변수
    private var cross: DataBaseHelper.CrossNode? = null

    // 터치 처리
    private lateinit var gestureDetector: GestureDetector

    // DB
    private lateinit var db1: DataBaseHelper
    private lateinit var db2: DataBaseHelper

    private lateinit var floorsIndoor: List<DataBaseHelper.IndoorFloor>
    private lateinit var nodesPlace: List<DataBaseHelper.PlaceNode>
    private lateinit var nodesCross: List<DataBaseHelper.CrossNode>
    private lateinit var nodesDanger: List<DataBaseHelper.DangerNode>

    // 길찾기
    private lateinit var dijk: Dijkstra

    private lateinit var root: List<Triple<Double, String, String>>

    // 건물 정보 기본 set
    private var placeid: Int = 1
    private var floorid: Int = 4

    // 지도 좌표 비율
    var ratio = 0F

    // mapvar
    var startId: Double? = null;
    var endId: Double? = null;

    // 터치 on/off
    var interaction: Boolean = true

    // 뒤로 가기 버튼
    private var doubleBackToExitPressedOnce = false

    // 딜레이 설정
    val handler = Handler()

    // 지도 크기
    var mScale = 0f

    // 바텀시트
    private lateinit var bottomSheetView : View
    private lateinit var bottomSheetDialog : BottomSheetDialog
    private lateinit var bottomSheetForwardBtn : Button
    private lateinit var bottomSheetBackwardBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출

        // DB
        db1 = DataBaseHelper(this, "Nodes1.db")
        db2 = DataBaseHelper(this, "Nodes2.db")

        nodesPlace = db2.getNodesPlace()
        nodesCross = db1.getNodesCross()
        floorsIndoor = db1.getFloorsIndoor()
        nodesDanger = db2.getNodesDanger()

        setContentView(R.layout.activity_main)

        // 지도
        map = findViewById(R.id.map)


        // 서치뷰, QR, 취소 버튼 레이아웃
        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById(R.id.searchView1)
        searchView2 = findViewById(R.id.searchView2)
        // 두번째 searchView와 취소버튼이 나타남에 따라 레이아웃 비율 조절.
        searchView_layout = findViewById(R.id.searchView_layout)

        // 취소 버튼
        cancel = findViewById(R.id.cancel)
        cancel.setBackgroundColor(Color.parseColor("#1188ff"))

        // 두번째 서치뷰와 취소 버튼 레이아웃
        svAndCancel = findViewById(R.id.svAndCancel)
        // 자동완성
        val listView1 = findViewById<ListView>(R.id.listView1)
        val listView2 = findViewById<ListView>(R.id.listView2)


        // 정보창
        info = findViewById(R.id.info)
        // 정보창에 띄울 정보들
        infoText1 = findViewById(R.id.text1)
        infoText2 = findViewById(R.id.text2)

        infoPic1 = findViewById(R.id.infoPic1)
        infoPic2 = findViewById(R.id.infoPic2)


        // 출발, 도착 버튼
        var start = findViewById<Button>(R.id.start)
        start.setBackgroundColor(Color.parseColor("#1188ff"))

        var end = findViewById<Button>(R.id.end)
        end.setBackgroundColor(Color.parseColor("#1188ff"))



        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)
        qrButton.setBackgroundColor(Color.parseColor("#1188ff"))


        // 층 수 스피너
        spinner = findViewById(R.id.spinner)


        // 지도 크기 제한
        map.maxScale = 1f

        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율

        // bottomSheet 선언
        bottomSheetView = layoutInflater.inflate(R.layout.activity_bottomsheet, null)
        bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.behavior.peekHeight = 600 // 펼치기 전 사이즈
        bottomSheetDialog.setContentView(bottomSheetView)


        // 자동 완성
        var autoComplete = ArrayList<String>()
        for(i in nodesPlace){
            if (i.nickname != "화장실" && i.nickname != "엘리베이터" && i.nickname != "출입문") {
                autoComplete.add(i.nickname)
            }
            autoComplete.add(i.name)
        }
        val ACadapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, autoComplete)
        listView1.adapter = ACadapter
        val autoCom = findViewById<FrameLayout>(R.id.autoCom)

        val autoCom2 = findViewById<FrameLayout>(R.id.autoCom2)
        listView2.adapter = ACadapter



        // 출발지와 목적지 입력 서치뷰 활성화.
        // 두번째 searchView 와 취소 버튼을 같이 나타나고 사라지게 조절.
        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                ACadapter.filter.filter(newText)
                if (newText.isEmpty() && searchView2.query.isEmpty()) {
                    if(!interaction){
                        setSearchLayout(View.VISIBLE)
                    }
                    else{
                        setSearchLayout(View.GONE)
                    }
                    // 입력창이 비어있으면 안보이게.
                    autoCom.visibility = View.GONE
                }
                else if(newText.isNotEmpty()){
                    autoCom.visibility = View.VISIBLE
                    if(newText == db2.searchPlace2(newText, nodesPlace)?.name){
                        autoCom.visibility = View.GONE
                    }
                }

                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty() && searchView2.query.isEmpty()) {
                    setSearchLayout(View.GONE)
                } else {
                    checkS1 = query
                    id = db2.searchPlace(query, nodesPlace)
                    if(id != null){

                        setSearchLayout(View.VISIBLE)
                        // 키보드 없애기
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)

                        floorid = id!!.id.toInt() / 100
                        spinner.setSelection(db1.findIdxtoFloor(floorid, floorsIndoor))

                        handler.postDelayed({
                            showInfo(id)
                        }, 500)
                    }
                    else{
                        Toast.makeText(applicationContext, "입력하신 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }

        })

        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                ACadapter.filter.filter(newText)
                // searchView2의 입력 상태에 따라 처리
                if (newText.isEmpty() && searchView1.query.isEmpty()) {
                    if(!interaction){
                        setSearchLayout(View.VISIBLE)
                    }
                    else{
                        setSearchLayout(View.GONE)
                    }
                    autoCom2.visibility = View.GONE
                } else {
                    setSearchLayout(View.VISIBLE)
                    autoCom2.visibility = View.VISIBLE
                    if(newText.isNotEmpty()){
                        autoCom2.visibility = View.VISIBLE
                        if(newText == db2.searchPlace2(newText, nodesPlace)?.name){
                            autoCom2.visibility = View.GONE
                        }
                    }
                    else if(newText.isEmpty()){
                        autoCom2.visibility = View.GONE
                    }
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                checkS2 = query
                id = db2.searchPlace(query, nodesPlace)
                if(id != null){
                    // 키보드 없애기
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)

                    floorid = id!!.id.toInt() / 100
                    spinner.setSelection(db1.findIdxtoFloor(floorid, floorsIndoor))

                    handler.postDelayed({
                        showInfo(id)
                    }, 500)
                }
                else{
                    Toast.makeText(applicationContext, "입력하신 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        })

        // 자동완성 아이템 선택
        listView1.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) // 선택된 아이템 가져오기
            // 선택된 아이템에 대한 처리 로직을 작성하세요
            // 예: 선택된 아이템을 텍스트뷰에 설정하거나 원하는 동작을 수행합니다
            searchView1.setQuery(selectedItem.toString(), true)
            // 자동완성 레이아웃을 숨깁니다
            autoCom.visibility = View.GONE
        }
        listView2.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) // 선택된 아이템 가져오기
            // 선택된 아이템에 대한 처리 로직을 작성하세요
            // 예: 선택된 아이템을 텍스트뷰에 설정하거나 원하는 동작을 수행합니다
            searchView2.setQuery(selectedItem.toString(), true)
            // 자동완성 레이아웃을 숨깁니다
            autoCom2.visibility = View.GONE
        }



        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        // 취소 버튼 활성화.
        cancel.setOnClickListener{
            spinner.isEnabled = true // 스피너 활성화 설정
            searchView2.setQuery("", false)
            searchView1.setQuery("", false)
            setSearchLayout(View.GONE)
            map.clearPin()

            map.cleanOtherPin("icon")

            addDanger(nodesDanger, floorid)

            startId = null
            endId = null

            interaction = true

            info_elvt.visibility = View.GONE
            detail.visibility = View.GONE


            btn_elvt.visibility = View.VISIBLE
            btn_back.visibility = View.GONE
        }



        // 화장실 검색 버튼
        var toiletButton: Button = findViewById(R.id.btn_toilet)
        toiletButton.setBackgroundColor(Color.parseColor("#1188ff"))

        // 출입문 검색 버튼
        var enterButton: Button = findViewById(R.id.btn_enter)
        enterButton.setBackgroundColor(Color.parseColor("#1188ff"))

        // 엘레베이터
        var elevatorButton: Button = findViewById(R.id.btn_elevator)
        elevatorButton.setBackgroundColor(Color.parseColor("#1188ff"))

        // 층수 값 n = 400~499
        // 화장실 찾기 버튼 활성화. //1
        toiletButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if (i.checkplace == 1 && i.id.toInt() / 100 == floorid)
                {
                    if (i.access == 0) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_1_0)
                    }
                    else if (i.access == 1) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_1_1)
                    }
                    else if (i.access == 2) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_1_2)
                    }
                }
            }
        }
        // 엘레베이터 찾기 버튼 활성화. //2
        elevatorButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if (i.checkplace == 2 && i.id.toInt() / 100 == floorid)
                {
                    if (i.access == 0) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_2_0)
                    }
                    else if (i.access == 1) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_2_1)
                    }
                    else if (i.access == 2) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_2_2)
                    }
                }
            }
        }
        // 출입문 찾기 버튼 활성화. //3
        enterButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if (i.checkplace == 3 && i.id.toInt() / 100 == floorid)
                {
                    if (i.access == 0) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_3_0)
                    }
                    else if (i.access == 1) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_3_1)
                    }
                    else if (i.access == 2) {
                        map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pin_3_2)
                    }
                }
            }
        }



        // 정보창 설정
        // 정보창 활성화 시 배경 가리기.
        info.setBackgroundResource(R.drawable.white_space)

        // 정보창과 map이 겹치는 부분을 클릭할 때 이벤트가 발생하지 않도록.
        info.setOnTouchListener { _, event ->
            // frameLayout을 터치할 때 이벤트가 발생하면 true를 반환하여
            // 해당 이벤트를 소비하고, map의 onTouchEvent를 호출하지 않도록 합니다.
            true
        }

        // 출발 버튼 누르면 searchView1 채우기
        start.setOnClickListener{
            if(checkS2 == id?.name || checkS2 == id?.nickname){
                searchView2.setQuery(null, false)
                endId = null
                map.clearEndPin()
                interaction = true
            }
            checkS1 = id?.name
            searchView1.setQuery(id?.name, false)
            setSearchLayout(View.VISIBLE)
            startId = id?.id
            map.clearStartPin()
            map.clearPin()
            map.addStartPin((PointF(id!!.x.toFloat()*ratio, id!!.y.toFloat()*ratio)),1, R.drawable.startpin)
            mapInit()
            info.visibility = View.GONE
        }

        // 도착 버튼 누르면 searchView2 채우기
        end.setOnClickListener{
            if(checkS1 == id?.name || checkS1 == id?.nickname){
                searchView1.setQuery(null, false)
                startId = null
                map.clearStartPin()
                interaction = true
            }
            checkS2 = id?.name
            searchView2.setQuery(id?.name, false)
            map.clearPin()
            map.clearEndPin()
            map.addEndPin((PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!)),1, R.drawable.finishpin)
            endId = id?.id
            mapInit()
            info.visibility = View.GONE
        }


        // 확대 축소 버튼
        val plus: Button = findViewById(R.id.plus)
        plus.setBackgroundColor(Color.parseColor("#1188ff"))

        val minus: Button = findViewById(R.id.minus)
        minus.setBackgroundColor(Color.parseColor("#1188ff"))


        plus.setOnClickListener{
            val visibleRect = Rect()
            map.visibleFileRect(visibleRect)
            val centerX = (visibleRect.left + visibleRect.right) / 2f
            val centerY = (visibleRect.top + visibleRect.bottom) / 2f
            mScale = map.scale
            mScale += 0.3f
            map.animateScaleAndCenter(mScale, PointF(centerX, centerY))?.start()
        }
        minus.setOnClickListener{
            val visibleRect = Rect()
            map.visibleFileRect(visibleRect)
            val centerX = (visibleRect.left + visibleRect.right) / 2f
            val centerY = (visibleRect.top + visibleRect.bottom) / 2f
            mScale = map.scale
            mScale -= 0.3f
            map.animateScaleAndCenter(mScale, PointF(centerX, centerY))?.start()
        }


        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.setBackgroundColor(Color.parseColor("#1188ff"))

        btn_emergency.setOnClickListener {
            showEmergencyPopup()
        }

        // 스피너에 항목 추가.
        val items: MutableList<String> = ArrayList()

        for (i in floorsIndoor) {
            if (i.placeid == placeid) {
                items.add(i.name)
            }
        }

        // 스피너 활성화
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setSelection(1)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var drawableName: String?
                var drawableId: Int?
                val selectedItem: String = parent.getItemAtPosition(position) as String
                if (selectedItem == "Add New Item") {
                    // Do something
                }


                var floorNum = selectedItem.substring(0,1).toInt()
                drawableName = db1.findMaptoFloor(floorNum, floorsIndoor)
                drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
                floorid = floorNum
                map.setImage(ImageSource.resource(drawableId))

                map.clearPin()
//                map.clearStartPin()
//                map.clearEndPin()
                map.cleanOtherPin("icon")
                map.clearPin("icon")
                addIcon(nodesPlace, floorid)
                addDanger(nodesDanger, floorid)
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }



        // 터치 이벤트
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                autoCom.visibility = View.GONE
                autoCom2.visibility = View.GONE
                // 키보드 없애기
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)
                if(interaction){
                    var pointt = map.viewToSourceCoord(e.x, e.y);
                    var x = pointt!!.x/ratio
                    var y = pointt!!.y/ratio

                    id = db2.findPlacetoXY(x.toInt(), y.toInt(), nodesPlace, floorid)

//                    var ppointt = map.pinTouchCheck(PointF(e.x, e.y))
//                    ppointt!!.x = ppointt.x / ratio
//                    ppointt.y = ppointt.y / ratio
//                    var pid = db.findPlacetoXY(ppointt.x.toInt(), ppointt.y.toInt(), nodesPlace, floorid)
//
//                    if (id != null)
//                    {
//                        map.clearPin()
//                        showInfo(id)
//                    }
//                    else if (pid != null)
//                    {
//                        map.clearPin()
//                        showInfo(pid)
//                    }
//                    else if(id == null){
//                        showInfo(null)
//                    }
//                    return true

                    if (id != null)
                    {
                        map.clearPin()
                        showInfo(id)
                    }
                    else if(id == null){
                        showInfo(null)
                    }
                    return true
                }
                else{
                    var pointt = map.viewToSourceCoord(e.x, e.y);
                    var x = pointt!!.x/ratio
                    var y = pointt!!.y/ratio
                    cross = db1.findCrosstoXY(x.toInt(), y.toInt(), nodesCross, floorid)
                    if (cross != null) {
                        for (i in root) {
                            if ((cross!!.id.toInt() % 100 > 70 && cross!!.id == i.first) || cross!!.id == startId || cross!!.id == endId) {
                                showCross(cross, root)
                            }
                        }
                    }
                    else if (cross == null) {
                        showCross(null, root)
                    }
//                    for(i in root){
//                        if(i.first == cross?.id){
//                            showCross(cross, root)
//                            return true
//                        }
//                    }
                    return true
                }
            }
        })
        map.setOnTouchListener(View.OnTouchListener { view, motionEvent -> // OnTouchListner로 터치 이벤트 감지
            gestureDetector.onTouchEvent( // gestureDectector로 터치 이벤트 처리
                motionEvent
            )
        })
    }


    // 서치뷰 레이아웃 조절
    fun setSearchLayout(v: Int){
        var layoutParams = searchView_layout.layoutParams as LinearLayout.LayoutParams
        if(v == View.VISIBLE){
            svAndCancel.visibility = View.VISIBLE
            layoutParams.weight = 3f
            searchView_layout.layoutParams = layoutParams
        }
        else{
            svAndCancel.visibility = View.GONE
            layoutParams.weight = 1.3f
            searchView_layout.layoutParams = layoutParams
        }
    }

    // 뒤로가기 버튼
    override fun onBackPressed() {
        if(info.visibility == View.VISIBLE){
            info.visibility = View.GONE
            return
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "뒤로 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    fun addIcon(nodesPlace: List<DataBaseHelper.PlaceNode>, floorId: Int) {
        for (i in nodesPlace) {
            if (i.id.toInt() / 100 == floorId) {
                if (i.checkplace == 1) {
                    if (i.access == 0) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_1_0, 2.0f, 2.0f, i.nickname)
                    }
                    else if (i.access == 1) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_1_1, 2.0f, 2.0f, i.nickname)
                    }
                    else {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_1_2, 2.0f, 2.0f, i.nickname)
                    }
                }
                else if (i.checkplace == 2) {
                    if (i.access == 0) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_2_0, 2.0f, 2.0f, i.nickname)
                    }
                    else if (i.access == 1) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_2_1, 2.0f, 2.0f, i.nickname)
                    }
                    else {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_2_2, 2.0f, 2.0f, i.nickname)
                    }
                }
                else if (i.checkplace == 3) {
                    if (i.access == 0) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_3_0, 2.0f, 2.0f, i.nickname)
                    }
                    else if (i.access == 1) {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_3_1, 2.0f, 2.0f, i.nickname)
                    }
                    else {
                        map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon_3_2, 2.0f, 2.0f, i.nickname)
                    }
                }
                else {
                    map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon, 2.0f, 2.0f, i.nickname)
                }
            }
        }
    }

    fun addDanger(nodesDanger: List<DataBaseHelper.DangerNode>, floorId: Int) {
        for (i in nodesDanger) {
            if (i.floorid == floorid) {
                if (i.checkdanger == 0) {
                    map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.stair, 2.0f, 2.0f, "")
                }
                else if (i.checkdanger == 1) {
                    if (i.access == 0) {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.ramp_0, 2.0f, 2.0f, "")
                    }
                    else {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.ramp_1, 2.0f, 2.0f, "")
                    }
                }
                else if (i.checkdanger == 2) {
                    if (i.access == 0) {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.bump_0, 2.0f, 2.0f, "")
                    }
                    else {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.bump_1, 2.0f, 2.0f, "")
                    }
                }
                else if (i.checkdanger == 3) {
                    if (i.access == 0) {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.barrier_0, 2.0f, 2.0f, "")
                    }
                    else {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.barrier_1, 2.0f, 2.0f, "")
                    }
                }
                else {
                    if (i.access == 0) {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.width_0, 2.0f, 2.0f, "")
                    }
                    else {
                        map.addPin("danger", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.width_1, 2.0f, 2.0f, "")
                    }
                }
            }
        }
    }


    // 길 찾기
    fun mapInit()
    {
        info_elvt = findViewById(R.id.info_elvt)
        detail = findViewById(R.id.detail)

        btn_back = findViewById(R.id.btn_back)
        btn_back.setBackgroundColor(Color.parseColor("#1188ff"))

        btn_elvt = findViewById(R.id.btn_elvt)
        btn_elvt.setBackgroundColor(Color.parseColor("#1188ff"))

        var scid = db1.findCrosstoID(startId, nodesCross)
        var ecid = db1.findCrosstoID(endId, nodesCross)

        var checklist = mutableListOf<Int>()

        var rootsub = mutableListOf<Triple<Double, String, String>>()

        var startfloor = 0
        var checkfloor = 0

        var check = 0
        var checkidx = 0


        if (startId != null && endId != null)
        {
            detail.visibility = View.VISIBLE
            interaction = false
            spinner.isEnabled = false // 스피너 비활성화 설정

            map.clearStartPin()
            map.clearEndPin()

            dijk = Dijkstra(nodesCross, startId!!, endId!!)
            root = dijk.findShortestPath(dijk!!.makeGraph())

            checklist.add(0)
            checkfloor = root[0].first.toInt() / 100

            for (i in root) {
                if (checkfloor != i.first.toInt() / 100) {
                    checklist.add(check)

                    checkfloor = i.first.toInt() / 100
                }

                check += 1
            }

            startfloor = root[0].first.toInt() / 100



            if (checklist.size == 1) {
                map.animateScaleAndCenter(1f,PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio))?.start()

                map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.startpin)
                map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.finishpin)

                makeLine(root)
            }
            else {
                info_elvt.visibility = View.VISIBLE

                rootsub = root.subList(checklist[checkidx], checklist[checkidx + 1]).toMutableList()

                spinner.setSelection(db1.findIdxtoFloor(startfloor, floorsIndoor))

                scid = db1.findCrosstoID(rootsub[0].first, nodesCross)
                ecid = db1.findCrosstoID(rootsub[rootsub.size - 1].first, nodesCross)

                startId = scid!!.id
                endId = ecid!!.id

                handler.postDelayed({
                    map.animateScaleAndCenter(1f,PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio))?.start()

                    map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.startpin)
                    map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.elvtpin)

                    makeLine(rootsub)
                }, 1000)

                btn_elvt.text = "${root[checklist[checkidx + 1]].first.toInt() / 100}층으로 이동"

                btn_elvt.setOnClickListener(){
                    checkidx += 1

                    if (checkidx == checklist.size - 1) {
                        rootsub = root.subList(checklist[checkidx], root.size).toMutableList()
                    }
                    else {
                        rootsub = root.subList(checklist[checkidx], checklist[checkidx + 1]).toMutableList()
                    }

                    spinner.setSelection(db1.findIdxtoFloor(rootsub[0].first.toInt() / 100, floorsIndoor))

                    scid = db1.findCrosstoID(rootsub[0].first, nodesCross)
                    ecid = db1.findCrosstoID(rootsub[rootsub.size - 1].first, nodesCross)

                    startId = scid!!.id
                    endId = ecid!!.id

                    handler.postDelayed({
                        map.animateScaleAndCenter(1f,PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio))?.start()

                        map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.elvtpin)

                        if (endId == root[root.size - 1].first) {
                            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.finishpin)
                        }
                        else {
                            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.elvtpin)
                        }

                        makeLine(rootsub)
                    }, 1000)

                    if (checkidx != 0) {
                        btn_back.visibility = View.VISIBLE
                    }
                    else {
                        btn_back.visibility = View.GONE
                    }

                    if (checkidx == checklist.size - 1) {
                        btn_elvt.visibility = View.GONE
                    }
                    else {
                        btn_elvt.visibility = View.VISIBLE
                        btn_elvt.text = "${root[checklist[checkidx + 1]].first.toInt() / 100}층으로 이동"
                    }
                }

                btn_back.setOnClickListener(){
                    checkidx -= 1

                    rootsub = root.subList(checklist[checkidx], checklist[checkidx + 1]).toMutableList()

                    spinner.setSelection(db1.findIdxtoFloor(rootsub[0].first.toInt() / 100, floorsIndoor))

                    scid = db1.findCrosstoID(rootsub[0].first, nodesCross)
                    ecid = db1.findCrosstoID(rootsub[rootsub.size - 1].first, nodesCross)

                    startId = scid!!.id
                    endId = ecid!!.id

                    handler.postDelayed({
                        map.animateScaleAndCenter(1f,PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio))?.start()

                        if (startId == root[0].first) {
                            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.startpin)
                        }
                        else {
                            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.elvtpin)
                        }

                        map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.elvtpin)

                        makeLine(rootsub)
                    }, 1000)

                    if (checkidx != 0) {
                        btn_back.visibility = View.VISIBLE
                    }
                    else {
                        btn_back.visibility = View.GONE
                    }

                    if (checkidx == checklist.size - 1) {
                        btn_elvt.visibility = View.GONE
                    }
                    else {
                        btn_elvt.visibility = View.VISIBLE
                        btn_elvt.text = "${root[checklist[checkidx + 1]].first.toInt() / 100}층으로 이동"
                    }
                }
            }
        }
    }



    // QR 촬영 후 데이터 값 받아옴.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
            val parts = returnedData!!.split(" ")
            placeid = parts[0].toInt()
            floorid = parts[1].toDouble().toInt() / 100

            spinner.setSelection(db1.findIdxtoFloor(floorid, floorsIndoor))

            id = db2.findPlacetoID(parts[1].toDouble(), nodesPlace)

            handler.postDelayed({
                showInfo(id)
            }, 600)

        }
    }


    // 비상 연락처 호출 함수
    private fun showEmergencyPopup() {
        val contactNumber = "02-910-4114"

        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.emergency_popup, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(popupView)
            .setCancelable(true)
            .create()

        val contactTextView = popupView.findViewById<TextView>(R.id.contactNumber)
        contactTextView.text = contactNumber

        popupView.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
            } else {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:$contactNumber")
                startActivity(intent)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    fun showInfo(id: DataBaseHelper.PlaceNode?) {
        if (id != null) {
            // 정보 사진
            infoPic1.setImageBitmap(id?.img1)
            infoPic2.setImageBitmap(id?.img2)

            // 접근성
            if(id?.access == 0){
                infoText2.setBackgroundColor(Color.RED)
            }
            else if(id?.access == 1){
                infoText2.setBackgroundColor(Color.YELLOW)
            }
            else if(id?.access == 2){
                infoText2.setBackgroundColor(Color.GREEN)
            }
            else {
                infoText2.setBackgroundColor(Color.LTGRAY)
            }

            // 지명
            if (id.checkplace == 0) {
                infoText1.setText("${id?.name} (${id.nickname})")
            }
            else {
                infoText1.setText(id?.name)
            }
            info.visibility = View.VISIBLE

            map.animateScaleAndCenter(1f,PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio))?.start()
            map.addPin(PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio), 1, R.drawable.pin)

            return
        }
        else{
            info.visibility = View.GONE
            map.clearPin()
        }
    }

    fun showCross(cross: DataBaseHelper.CrossNode?, root: List<Triple<Double, String, String>>) {

        var bsText : TextView = bottomSheetView.findViewById(R.id.bsText)
        var bsImage : ImageView = bottomSheetView.findViewById(R.id.bsImage)
        var bsArrow : ImageView = bottomSheetView.findViewById(R.id.bsArrowImage)
        var crossIndex = -1
        bottomSheetForwardBtn = bottomSheetView.findViewById(R.id.bs_btn_forward)
        bottomSheetForwardBtn.setBackgroundColor(Color.parseColor("#1188ff"))

        bottomSheetBackwardBtn = bottomSheetView.findViewById(R.id.bs_btn_backward)
        bottomSheetBackwardBtn.setBackgroundColor(Color.parseColor("#1188ff"))

        bottomSheetForwardBtn.setOnClickListener(){
            var next_cross = db1.findCrosstoID(root[crossIndex+1].first, nodesCross)

            if (cross!!.id == endId) {
                btn_elvt.performClick()

                handler.postDelayed({
                    showCross(db1.findCrosstoID(startId, nodesCross), root)
                }, 2000)
            }
            else {
                while (next_cross!!.id % 100 <= 70 && crossIndex < root.size)
                {
                    if (next_cross!!.id == endId) {
                        break
                    }

                    crossIndex = crossIndex + 1
                    next_cross = db1.findCrosstoID(root[crossIndex+1].first, nodesCross)
                }

                if (next_cross != null) {
                    showCross(next_cross, root)
                }
                else if (next_cross == null) {
                    showCross(null, root)
                }
            }
        }
        bottomSheetBackwardBtn.setOnClickListener() {
            var prev_cross = db1.findCrosstoID(root[crossIndex-1].first, nodesCross)

            if (cross!!.id == startId) {
                btn_back.performClick()

                handler.postDelayed({
                    showCross(db1.findCrosstoID(endId, nodesCross), root)
                }, 2000)
            }
            else {
                while ((prev_cross!!.id % 100 <= 70) && (crossIndex > -1))
                {
                    if (prev_cross!!.id == startId) {
                        break
                    }

                    crossIndex = crossIndex - 1
                    prev_cross = db1.findCrosstoID(root[crossIndex-1].first, nodesCross)
                }
                if (prev_cross != null) {
                    showCross(prev_cross, root)
                }
                else if (prev_cross == null) {
                    showCross(null, root)
                }
            }
        }
        var elvt = 0

        if (cross != null) {
            if (cross.id == root[0].first) {
                bsImage.setImageBitmap(db2.findPlacetoID(cross.id, nodesPlace)!!.img1)
                bsText.setText("현재위치에서 출발")
                bsArrow.setImageResource(0)
                crossIndex = 0
            }
            else if (cross.id == root[root.size - 1].first) {
                bsImage.setImageBitmap(db2.findPlacetoID(cross.id, nodesPlace)!!.img1)
                bsText.setText("목적지 도착")
                bsArrow.setImageResource(0)
                crossIndex = root.size - 1
            }
            else {
                for (index in root.indices) {
                    var i = root[index]
                    if (cross.id == i.first) {
                        crossIndex = index
                        // 정보 사진
                        var check = choiceArrow(i.second, i.third)

                        if (i.second == "east") {
                            bsImage.setImageBitmap(cross.imgEast)
                            bsText.setText(cross.name + "에서 " + check.second)
                        } else if (i.second == "west") {
                            bsImage.setImageBitmap(cross.imgWest)
                            bsText.setText(cross.name + "에서 " + check.second)
                        } else if (i.second == "south") {
                            bsImage.setImageBitmap(cross.imgSouth)
                            bsText.setText(cross.name + "에서 " + check.second)
                        } else if (i.second == "north") {
                            bsImage.setImageBitmap(cross.imgNorth)
                            bsText.setText(cross.name + "에서 " + check.second)
                        }

                        bsArrow.setImageResource(check.first)

                        break
                    }
                }

                if (cross.id == startId) {
                    bsImage.setImageBitmap(db2.findPlacetoID(cross.id, nodesPlace)!!.img1)
                    bsText.setText("엘리베이터 하차")
                    bsArrow.setImageResource(0)
                }
                else if (cross.id == endId) {
                    bsImage.setImageBitmap(db2.findPlacetoID(cross.id, nodesPlace)!!.img1)
                    bsText.setText("엘리베이터 탑승")
                    bsArrow.setImageResource(0)
                }
            }
        }
        else {return}

        map.animateScaleAndCenter(1f,PointF(cross.x.toFloat()*ratio, cross.y.toFloat()*ratio))?.start()

        if (crossIndex == root.size - 1)
        {bottomSheetForwardBtn.visibility = View.GONE}
        else if (bottomSheetForwardBtn.visibility == View.GONE) {
            bottomSheetForwardBtn.visibility = View.VISIBLE
        }
        if (crossIndex == 0)
        {bottomSheetBackwardBtn.visibility = View.GONE}
        else if (bottomSheetBackwardBtn.visibility == View.GONE){
            bottomSheetBackwardBtn.visibility = View.VISIBLE
        }

        bottomSheetDialog.show()

        return

    }

    fun choiceArrow(second: String, third: String): Pair<Int, String> {
        if (second == "east") {
            if (third == "south") {
                return Pair(R.drawable.east_arrow, "우회전")
            }
            else if (third == "north") {
                return Pair(R.drawable.west_arrow, "좌회전")
            }
            else {
                return Pair(R.drawable.north_arrow, "직진")
            }
        }
        else if (second == "west") {
            if (third == "north") {
                return Pair(R.drawable.east_arrow, "우회전")
            }
            else if (third == "south") {
                return Pair(R.drawable.west_arrow, "좌회전")
            }
            else {
                return Pair(R.drawable.north_arrow, "직진")
            }
        }
        else if (second == "south") {
            if (third == "west") {
                return Pair(R.drawable.east_arrow, "우회전")
            }
            else if (third == "east") {
                return Pair(R.drawable.west_arrow, "좌회전")
            }
            else {
                return Pair(R.drawable.north_arrow, "직진")
            }
        }
        else if (second == "north") {
            if (third == "east") {
                return Pair(R.drawable.east_arrow, "우회전")
            }
            else if (third == "west") {
                return Pair(R.drawable.west_arrow, "좌회전")
            }
            else {
                return Pair(R.drawable.north_arrow, "직진")
            }
        }
        else {
            return Pair(0, "")
        }
    }

    fun makeLine(root: List<Triple<Double, String, String>>) {
        for (index in root.indices)
        {
            val element = root[index]

            var pointt = db1.findCrosstoID(element.first, nodesCross!!)
            var tempX = pointt!!.x.toFloat()*ratio
            var tempY = pointt!!.y.toFloat()*ratio

            if (index != root.size - 1) {
                if (db1.findCrosstoID(root[index].first, nodesCross)!!.access == 1 && db1.findCrosstoID(root[index + 1].first, nodesCross)!!.access == 1) {
                    map.addLine(PointF(tempX, tempY), Color.YELLOW)
                }
                else {
                    map.addLine(PointF(tempX, tempY), Color.GREEN)
                }
            }
            else {
                map.addLine(PointF(tempX, tempY), Color.GREEN)
            }

            if ((element.first % 100) > 70) {
                if (element.third == "east") {
                    map.addPin("arrow", PointF(tempX, tempY), 1, R.drawable.east_arrow,2.0f, 2.0f)
                }
                else if (element.third == "west") {
                    map.addPin("arrow", PointF(tempX, tempY), 1, R.drawable.west_arrow, 2.0f, 2.0f)
                }
                else if (element.third == "south") {
                    map.addPin("arrow", PointF(tempX, tempY), 1, R.drawable.south_arrow, 2.0f, 2.0f)
                }
                else if (element.third == "north") {
                    map.addPin("arrow", PointF(tempX, tempY), 1, R.drawable.north_arrow, 2.0f, 2.0f)
                }
            }
        }
    }
}