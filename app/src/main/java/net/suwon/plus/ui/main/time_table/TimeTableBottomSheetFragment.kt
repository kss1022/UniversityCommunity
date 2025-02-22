package net.suwon.plus.ui.main.time_table

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.size
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.suwon.plus.R
import net.suwon.plus.data.entity.timetable.*
import net.suwon.plus.databinding.TimeTableBottomSheetBinding
import net.suwon.plus.ui.main.time_table.customcolor.CustomTableColorCategory700
import net.suwon.plus.ui.main.time_table.customcolor.CustomTableColorCategoryA100
import net.suwon.plus.widget.adapter.ColorAdapter

class TimeTableBottomSheetFragment() : BottomSheetDialogFragment() {

    private var _binding: TimeTableBottomSheetBinding? = null
    val binding get() = _binding!!

    private lateinit var currentTimeTableWithCell: TimeTableWithCell
    private lateinit var editTimeTableCell: TimeTableCellEntity


    private val addedLocationAndTimeViewList =
        arrayListOf<Triple<TimeTableLocationAndTime, Int, Int>>()


    private var baseTableColor = -1
    private var baseTextColor = R.color.colorPrimaryVariant

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(
            DialogFragment.STYLE_NORMAL,
            R.style.Widget_Suwon_University_Community_BottomSheetDialog
        )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TimeTableBottomSheetBinding.inflate(layoutInflater)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timeTableWithCell = arguments?.getParcelable<TimeTableWithCell>(TABLE_WITH_CELL)


        if (timeTableWithCell == null) {
            this@TimeTableBottomSheetFragment.dismiss()
        } else {
            currentTimeTableWithCell = timeTableWithCell
        }


        val tableCell = arguments?.getParcelable(ENTITY) as TimeTableCellEntity?


        if (tableCell != null) {
            editTimeTableCell = tableCell
            initViews()
        }


        bindView()
    }


    private fun initViews() = with(binding) {
        lectureNameEditText.setText(editTimeTableCell.name)
        professorNameEditText.setText(editTimeTableCell.professorName)
        baseTableColor = editTimeTableCell.cellColor
        baseTextColor = editTimeTableCell.textColor

        tableColorButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                editTimeTableCell.cellColor
            )
        )
        textColorButton.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                editTimeTableCell.textColor
            )
        )

        editTimeTableCell.locationAndTimeList.forEach {
            addLocationAndTimeView(it, null)
        }
    }


    private fun bindView() = with(binding) {
        addLocationAndTimeTextView.setOnClickListener {
            showEditAlertDialog(null)
        }


        cancelButton.setOnClickListener {
            this@TimeTableBottomSheetFragment.dismiss()
        }


        deleteButton.setOnClickListener {
            if (::editTimeTableCell.isInitialized) {
                showDeleteAlertDialog()
            } else {
                this@TimeTableBottomSheetFragment.dismiss()
            }
        }


        textColorButton.setOnClickListener{
            showTextColorAlertDialog()
        }

        tableColorButton.setOnClickListener {
            showColorAlertDialog()
        }

        confirmButton.setOnClickListener {
            if (::editTimeTableCell.isInitialized) {
                itemClickListener(
                    TimeTableCellEntity(
                        cellId = editTimeTableCell.cellId,
                        name = lectureNameEditText.text.toString(),
                        distinguish = editTimeTableCell.distinguish,
                        point = editTimeTableCell.point,
                        locationAndTimeList = addedLocationAndTimeViewList.map { it.first }
                            .sortedBy { it.day }.sortedBy { it.time.first },
                        professorName = professorNameEditText.text.toString(),
                        cellColor = baseTableColor,
                        textColor = baseTextColor
                    ), EDIT
                )

            } else {
                //새로운 강의 추가 - 색상을 고르지 않은 경우
                if (baseTableColor == -1) {

                    val colorList = TableColorCategory.values().toMutableList()

                    currentTimeTableWithCell.timeTableCellList.forEach { newCell ->
                        if (newCell.locationAndTimeList.isNullOrEmpty().not()) {
                            colorList.find { it.colorId == newCell.cellColor }?.let {
                                colorList.remove(colorList.find { it.colorId == newCell.cellColor })
                            }
                        }
                    }

                    if (colorList.isEmpty()) {
                        baseTableColor = TableColorCategory.values().random().colorId
                    } else {
                        //baseTableColor = colorList[Random.nextInt(colorList.size)].colorId
                        baseTableColor = colorList.first().colorId
                    }
                }

                itemClickListener(
                    TimeTableCellEntity(
                        cellId = System.currentTimeMillis(),
                        name = lectureNameEditText.text.toString(),
                        distinguish = "",
                        point = 0f,
                        locationAndTimeList = addedLocationAndTimeViewList.map { it.first },
                        professorName = professorNameEditText.text.toString(),
                        cellColor = baseTableColor,
                        textColor =  baseTextColor
                    ), NEW
                )
            }



            this@TimeTableBottomSheetFragment.dismiss()
        }
    }


    private fun showDeleteAlertDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("정말 삭제하시겠어요?")
            .setPositiveButton("확인") { dialog, _ ->
                itemClickListener(editTimeTableCell, DELETE)
                dialog.dismiss()
                this@TimeTableBottomSheetFragment.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }




    private fun showColorAlertDialog() {
        val alertDialog = AlertDialog.Builder(requireContext()).create()


        val colorList = mutableListOf<List<Int >>()
        var position = 0

        colorList.add(TableColorCategory.values().map { it.colorId })
        colorList.add(CustomTableColorCategory700.values().map { it.colorId })
        colorList.add(CustomTableColorCategoryA100.values().map { it.colorId })


        val colorAdapter = ColorAdapter(colorList[position]) {
            Log.e("Color Id", "$it")
            binding.tableColorButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    it
                )
            )
            baseTableColor = it
            alertDialog.dismiss()
        }

        val colorView = LayoutInflater.from(requireContext())
            .inflate(R.layout.time_table_color_alert_dialog, null).apply {
                this.findViewById<RecyclerView>(R.id.colorRecyclerView).apply {
                    adapter = colorAdapter
                }

                this.findViewById<ImageView>(R.id.rightButton).setOnClickListener {
                    if(position == colorList.size-1) position = 0 else position++
                    colorAdapter.submitColor( colorList[position])
                }

                this.findViewById<ImageView>(R.id.leftButton).setOnClickListener {
                    if(position == 0) position = colorList.size-1 else position--
                    colorAdapter.submitColor( colorList[position])
                }
            }

        alertDialog.setView(colorView)
        alertDialog.show()
    }


    private fun showTextColorAlertDialog() {
        val alertDialog = AlertDialog.Builder(requireContext()).create()

        //todo 글자색상 생각
        val colorList = listOf<Int>(R.color.black, R.color.white, R.color.gray)


        val colorAdapter = ColorAdapter(colorList) {
            Log.e("Color Id", "$it")
            binding.textColorButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    it
                )
            )
            baseTextColor = it
            alertDialog.dismiss()
        }

        val colorView = LayoutInflater.from(requireContext())
            .inflate(R.layout.time_table_color_alert_dialog, null).apply {
                this.findViewById<RecyclerView>(R.id.colorRecyclerView).apply {
                    adapter = colorAdapter
                }

                this.findViewById<ImageView>(R.id.rightButton).isGone = true

                this.findViewById<ImageView>(R.id.leftButton).isGone = true
            }

        alertDialog.setView(colorView)
        alertDialog.show()
    }


    /**
     * AlertDialog
     * positiveButton click- > [checkData] -> [checkOverlapTimeAndAdd]
     *
     */
    @SuppressLint("InflateParams")
    private fun showEditAlertDialog(editLocationAndTime: TimeTableLocationAndTime?) {
        val alertDialog = AlertDialog.Builder(requireContext()).create()

        val alertDialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.time_table_edit_alert_dialog, null).apply {
                val startTimeEditText = this.findViewById<EditText>(R.id.startTimeEditText)

                startTimeEditText.apply {
                    setTimeEditTextListener(startTimeEditText)
                }

                val endTimeEditText = this.findViewById<EditText>(R.id.endTimeEditText)
                endTimeEditText.apply {
                    setTimeEditTextListener(endTimeEditText)
                }


                this.findViewById<TextView>(R.id.positiveButton).setOnClickListener {
                    if (checkData(this, editLocationAndTime)) {
                        alertDialog.dismiss()
                    }
                }

                this.findViewById<TextView>(R.id.negativeButton).setOnClickListener {
                    alertDialog.dismiss()
                }


                editLocationAndTime?.let {
                    findViewById<EditText>(R.id.locationEditText).setText(it.location)
                    val startHour = it.time.first / 60
                    val startMinutes = it.time.first - startHour * 60
                    val endHour = it.time.second / 60
                    val endMinutes = it.time.second - endHour * 60

                    startTimeEditText.setText(
                        getString(
                            R.string.time_format,
                            String.format("%02d", startHour),
                            String.format("%02d", startMinutes)
                        )
                    )
                    endTimeEditText.setText(
                        getString(
                            R.string.time_format,
                            String.format("%02d", endHour),
                            String.format("%02d", endMinutes)
                        )
                    )

                    when (it.day) {
                        DayOfTheWeek.MON -> {
                            findViewById<CheckBox>(R.id.monCheckBox).isChecked = true
                        }
                        DayOfTheWeek.TUE -> {
                            findViewById<CheckBox>(R.id.tueCheckBox).isChecked = true
                        }
                        DayOfTheWeek.WED -> {
                            findViewById<CheckBox>(R.id.wedCheckBox).isChecked = true
                        }
                        DayOfTheWeek.THU -> {
                            findViewById<CheckBox>(R.id.thuCheckBox).isChecked = true
                        }
                        DayOfTheWeek.FRI -> {
                            findViewById<CheckBox>(R.id.friCheckBox).isChecked = true
                        }
                        else -> Unit
                    }
                }

            }

        alertDialog.apply {
            setView(alertDialogView)
            show()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun EditText.setTimeEditTextListener(editText: EditText) {
        addTextChangedListener {

            when (it.toString().length) {
                1 -> {
                    if (Character.getNumericValue(it.toString()[0]) > 2) {
                        editText.setText("")
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.time_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                2 -> {

                    if (Character.getNumericValue(it.toString()[0]) == 2 && Character.getNumericValue(
                            it.toString()[1]
                        ) > 3
                    ) {
                        editText.setText(it.toString()[0].toString())
                        editText.setSelection(1)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.time_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        editText.setText("${editText.text}:")
                        editText.setSelection(3)
                    }
                }

                4 -> {
                    if (Character.getNumericValue(it.toString()[3]) > 5) {
                        editText.setText(
                            editText.text.substring(0, 2)
                        )
                        editText.setSelection(3)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.time_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
                else -> Unit
            }
        }

        setOnKeyListener { _, _, keyEvent ->

            if (keyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                editText.setText("")
            }

            return@setOnKeyListener false
        }
    }

    //시간이 겹치는지 확인해준다. 겹치면 Toast 겹치지 않으면 뷰에 시간을 추가해준다.
    private fun checkData(view: View, editLocationAndTime: TimeTableLocationAndTime?): Boolean {
        val startEditText = view.findViewById<EditText>(R.id.startTimeEditText).text
        if (startEditText.length < 5) {
            Toast.makeText(requireContext(), getString(R.string.time_is_empty), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        val endEditText = view.findViewById<EditText>(R.id.endTimeEditText).text


        if (endEditText.length < 5 || endEditText.substring(0, 2)
                .toInt() >= 24 || endEditText.substring(3, 5).toInt() >= 60
        ) {
            Toast.makeText(requireContext(), getString(R.string.time_is_empty), Toast.LENGTH_SHORT)
                .show()
            return false
        }


        val startTime =
            startEditText.substring(0, 2).toInt() * 60 + startEditText.substring(3, 5).toInt()
        val endTime = endEditText.substring(0, 2).toInt() * 60 + endEditText.substring(3, 5).toInt()


        if (startTime >= endTime) {
            Toast.makeText(requireContext(), getString(R.string.time_is_empty), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        val dayList = arrayListOf<Char>()

        if (view.findViewById<CheckBox>(R.id.monCheckBox).isChecked) dayList.add('월')
        if (view.findViewById<CheckBox>(R.id.tueCheckBox).isChecked) dayList.add('화')
        if (view.findViewById<CheckBox>(R.id.wedCheckBox).isChecked) dayList.add('수')
        if (view.findViewById<CheckBox>(R.id.thuCheckBox).isChecked) dayList.add('목')
        if (view.findViewById<CheckBox>(R.id.friCheckBox).isChecked) dayList.add('금')

        if (dayList.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.day_is_empty), Toast.LENGTH_SHORT)
                .show()
            return false
        }

        val location = view.findViewById<EditText>(R.id.locationEditText).text.toString()

        if (checkOverlapTimeAndAdd(location, dayList, startTime, endTime, editLocationAndTime).not()
        ) {
            return false
        }
        return true
    }


    private fun checkOverlapTimeAndAdd(
        location: String,
        dayList: ArrayList<Char>,
        startTime: Int,
        endTime: Int,
        editLocationAndTime: TimeTableLocationAndTime?
    ): Boolean {
        val addedList = currentTimeTableWithCell.timeTableCellList

        val selectedLocationAndTimeList = dayList.map {
            TimeTableLocationAndTime(
                location = location,
                day = it.toDayOfTheWeek(),
                startTime to endTime
            )
        }

        val overlappingSet = mutableSetOf<TimeTableLocationAndTime>()


        selectedLocationAndTimeList.forEach { selected ->
            addedList.forEach { added ->
                added.locationAndTimeList.forEach { addedLocationAndTime ->
                    if (selected.day == addedLocationAndTime.day) {
                        if ((addedLocationAndTime.time.first > selected.time.second ||
                                    addedLocationAndTime.time.second < selected.time.first).not()
                        ) {
                            overlappingSet.add(addedLocationAndTime)
                        }
                    }
                }
            }

            addedLocationAndTimeViewList.forEach { addedLocationAndTime ->
                if (selected.day == addedLocationAndTime.first.day) {
                    if ((addedLocationAndTime.first.time.first > selected.time.second ||
                                addedLocationAndTime.first.time.second < selected.time.first).not()
                    ) {
                        overlappingSet.add(addedLocationAndTime.first)
                    }
                }
            }
        }

        //수정중인 항목과 겹치는 경우 제외 시켜준다.
        editLocationAndTime?.let { edit ->
            val editTime = overlappingSet.filter { set ->
                (set.day == edit.day) &&
                        ((set.time.first > edit.time.second || set.time.second < edit.time.first).not())
            }
            editTime.forEach {
                overlappingSet.remove(it)
            }
        }

        return if (overlappingSet.isNotEmpty()) {
            val overlapTimeStr =
                overlappingSet.first().day.char + overlappingSet.first().getTimeString()

            Toast.makeText(requireContext(), "겹치는 시간이 있습니다😱\n$overlapTimeStr", Toast.LENGTH_SHORT)
                .show()
            false
        } else {

            var position: Int? = null
            editLocationAndTime?.let { editView ->
                position = removeAddedView(editView)
            }

            selectedLocationAndTimeList.forEach {
                addLocationAndTimeView(it, position)
                position?.let { position = it + 1 }
            }
            true
        }
    }


    @SuppressLint("InflateParams")
    private fun addLocationAndTimeView(
        timeTableLocationAndTime: TimeTableLocationAndTime,
        editPos: Int?
    ) {
        val dayAndLocationTextView = LayoutInflater.from(requireContext())
            .inflate(R.layout.time_table_bottom_sheet_loacation_time, null)
        dayAndLocationTextView.apply {
            this.findViewById<TextView>(R.id.locationTextView).apply {
                text = timeTableLocationAndTime.location

                setOnClickListener {
                    showEditAlertDialog(timeTableLocationAndTime)
                }
            }



            this.findViewById<TextView>(R.id.timeTextView).apply {
                text = getString(
                    R.string.day_and_time,
                    timeTableLocationAndTime.day.char,
                    timeTableLocationAndTime.getTimeString()
                )
                setOnClickListener {
                    showEditAlertDialog(timeTableLocationAndTime)
                }
            }


            this.findViewById<ImageView>(R.id.editButton).setOnClickListener {
                showEditAlertDialog(timeTableLocationAndTime)
            }

            id = ViewCompat.generateViewId()

            val deleteButton = this.findViewById<ImageView>(R.id.deleteButton).apply {
                id = ViewCompat.generateViewId()

                setOnClickListener { view ->
                    val clickView = addedLocationAndTimeViewList.find { it.third == view?.id }
                    binding.linearLayout.removeView(binding.linearLayout.findViewById(clickView!!.second))
                    addedLocationAndTimeViewList.remove(clickView)
                }
            }

            editPos?.let {
                addedLocationAndTimeViewList.add(
                    it,
                    Triple(timeTableLocationAndTime, dayAndLocationTextView.id, deleteButton.id)
                )
            } ?: kotlin.run {
                addedLocationAndTimeViewList.add(
                    Triple(timeTableLocationAndTime, dayAndLocationTextView.id, deleteButton.id)
                )
            }
        }



        editPos?.let {
            if (binding.linearLayout.size == 0) {
                binding.linearLayout.addView(dayAndLocationTextView, 0)
            } else {
                binding.linearLayout.addView(dayAndLocationTextView, it)
            }
        } ?: kotlin.run {
            binding.linearLayout.addView(dayAndLocationTextView)
        }
    }


    private fun removeAddedView(timeTableLocationAndTime: TimeTableLocationAndTime): Int? {
        val size = addedLocationAndTimeViewList.size

        for (i in 0 until size) {
            if (addedLocationAndTimeViewList[i].first == timeTableLocationAndTime) {

                binding.linearLayout.removeView(
                    binding.linearLayout.findViewById(
                        addedLocationAndTimeViewList[i].second
                    )
                )
                addedLocationAndTimeViewList.removeAt(i)
                return i
            }

        }
        return null
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    companion object {
        fun newInstance(
            timeTableWithCell: TimeTableWithCell,
            timeTableCellEntity: TimeTableCellEntity?,
            itemClickListener: (TimeTableCellEntity?, Int) -> Unit
        ): TimeTableBottomSheetFragment {
            this.itemClickListener = itemClickListener

            return TimeTableBottomSheetFragment().apply {
                arguments = bundleOf(
                    TABLE_WITH_CELL to timeTableWithCell,
                    ENTITY to timeTableCellEntity,
                )
            }
        }

        lateinit var itemClickListener: (TimeTableCellEntity?, Int) -> Unit

        const val TAG = "TimeTableBottomSheet"
        const val ENTITY = "TimeTableEntity"
        const val TABLE_WITH_CELL = "TimeTableWithCell"

        const val NEW = 1000
        const val EDIT = 1001
        const val DELETE = 1002
    }
}