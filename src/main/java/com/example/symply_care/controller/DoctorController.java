package com.example.symply_care.controller;

import com.example.symply_care.dto.DoctorDTO;
import com.example.symply_care.dto.PatientDTO;
import com.example.symply_care.entity.Appointments;
import com.example.symply_care.entity.Inquiries;
import com.example.symply_care.entity.Users;
import com.example.symply_care.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor

public class DoctorController {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private DoctorService doctorService;


    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @PostMapping("/addDoctor")
    public ResponseEntity<DoctorDTO> createDoctor(@RequestBody @Valid DoctorDTO doctorDTO) throws Exception {
        return ResponseEntity.ok(doctorService.createDoctor(doctorDTO));
    }


    @GetMapping("/doctor/I{id}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(doctorService.getDoctorByID(id));
    }
    @GetMapping("/doctor/E{email}")
    public ResponseEntity<DoctorDTO> getDoctorByEmail(@PathVariable String email) throws Exception {
        return ResponseEntity.ok(doctorService.getDoctorByEmail(email));
    }

    @PutMapping("/updateDoctor/{id}")
    public ResponseEntity<DoctorDTO> updateDoctor(@PathVariable Long id, @RequestBody @Valid DoctorDTO doctorDTO) throws Exception
    {
        return ResponseEntity.ok(doctorService.updateDoctor(id, doctorDTO));
    }

    @DeleteMapping("/deleteDoctor/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        try {
            doctorService.deleteDoctor(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/doctor/{id}/patients")
    public ResponseEntity<List<PatientDTO>> getPatientsOfDoctor(@PathVariable Long id){
        return ResponseEntity.ok(doctorService.getPatientsOfDoctor(id));
    }


    @GetMapping("/doctor/{id}/inquiries")
    public ResponseEntity<List<Inquiries>> getInquiriesOfDoctor(@PathVariable Long id){
        return ResponseEntity.ok(doctorService.getInquiriesOfDoctor(id));
    }
    @GetMapping("/doctor/{id}/appointments")
    public ResponseEntity<List<Appointments>> getAppointmentsOfDoctor(@PathVariable Long id){
        return ResponseEntity.ok(doctorService.getAppointmentsOfDoctor(id));
    }

    @PostMapping("/doctor/{doctorID}/addPatient")
    public ResponseEntity<List<PatientDTO>> addPatientToDoctor(@PathVariable Long doctorID,@RequestBody @Valid Long patientID){
        return ResponseEntity.ok(doctorService.addPatientToDoctor(doctorID,patientID));
    }

    @PostMapping("/doctor/{doctorID}/addAppointment")
    public ResponseEntity<List<Appointments>> addAppointmentToDoctor(@PathVariable Long doctorID,@RequestBody @Valid Long patientID,@RequestBody @Valid Date date ){
        return ResponseEntity.ok(doctorService.addAppointmentToDoctor(doctorID,patientID,date));
    }
    @PostMapping("/doctor/{doctorID}/addRole")
    public ResponseEntity<Users> addRoleToDoctor(@PathVariable Long doctorID, @RequestBody @Valid String role){
        return ResponseEntity.ok(doctorService.addRoleToDoctor(doctorID,role));
    }

    @PostMapping("/doctor/{doctorID}/addImage")
    public String uploadImage(@PathVariable Long doctorID, @RequestParam("image") MultipartFile file) {
        System.out.println("---------------------------------------");
        System.out.println(file);
        try {
            doctorService.uploadImage(doctorID,file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/doctor/{doctorID}";
    }


}
