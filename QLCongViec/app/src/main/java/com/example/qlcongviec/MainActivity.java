package com.example.qlcongviec;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText edtCongViec, edtNoiDung;
    TextView txtNgayCongViec, txtGioCongViec;
    Button btnThemCongViec, btnChonGio, btnChonNgay;
    ListView lvDanhSachCongViec;
    ArrayAdapter adapter;
    List<String> listCongViec = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapping();
        adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, listCongViec);
        lvDanhSachCongViec.setAdapter(adapter);
        btnChonNgay.setOnClickListener(this);
        btnChonGio.setOnClickListener(this);
        btnThemCongViec.setOnClickListener(this);

    }

    private void mapping() {
        edtCongViec = findViewById(R.id.edtCongViec);
        edtNoiDung = findViewById(R.id.edtNoiDung);
        txtNgayCongViec = findViewById(R.id.txtNgayCongViec);
        txtGioCongViec = findViewById(R.id.txtGioCongViec);
        btnThemCongViec = findViewById(R.id.btnThemCongViec);
        btnChonGio = findViewById(R.id.btnChonGio);
        btnChonNgay = findViewById(R.id.btnChonNgay);
        lvDanhSachCongViec = findViewById(R.id.lvDanhSachCongViec);
    }

    @Override
    public void onClick(View view) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        switch (view.getId()) {
            case R.id.btnChonNgay:
                DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Calendar chooseDate = Calendar.getInstance();
                        chooseDate.set(i, i1, i2);
                        String strDate = simpleDateFormat.format(chooseDate.getTime());
                        txtNgayCongViec.setText(strDate);
                    }
                }, year, month, day);
                datePickerDialog.show();
                break;
            case R.id.btnChonGio:
                TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm:ss");
                        Calendar chooseTime = Calendar.getInstance();
                        chooseTime.set(0, 0, 0, i, i1);
                        String strTime = simpleTimeFormat.format(chooseTime.getTime());
                        txtGioCongViec.setText(strTime);
                    }
                }, hour, minute, true);
                timePickerDialog.show();
                break;
            case R.id.btnThemCongViec:
                String tenCongViec = edtCongViec.getText().toString();
                String ngayCongViec = txtNgayCongViec.getText().toString();
                String gioCongViec = txtGioCongViec.getText().toString();
                listCongViec.add(tenCongViec + " - " + ngayCongViec + " " + gioCongViec);
                adapter.notifyDataSetChanged();
                clearValues();
        }
    }

    private void clearValues() {
        edtCongViec.setText("");
        edtNoiDung.setText("");
        txtNgayCongViec.setText("");
        txtGioCongViec.setText("");
    }
}