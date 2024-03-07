package com.example.symply_care.service;

import com.example.symply_care.dto.DoctorDTO;
import com.example.symply_care.dto.PatientDTO;
import com.example.symply_care.entity.*;
import com.example.symply_care.repository.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Valid
public class PatientService {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    private final UsersRepository usersRepository;
    private final RoleRepository roleRepository;
    private final InquiriesRepository inquiriesRepository;
    private final AppointmentsRepository appointmentsRepository;
    @Autowired
    @Lazy
    private DoctorService doctorService;


    public PatientService(PatientRepository patientRepository, DoctorRepository doctorRepository, UsersRepository usersRepository, RoleRepository roleRepository, InquiriesRepository inquiriesRepository, AppointmentsRepository appointmentsRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.usersRepository = usersRepository;
        this.roleRepository=roleRepository;
        this.inquiriesRepository = inquiriesRepository;
        this.appointmentsRepository = appointmentsRepository;
    }

    @Transactional
    public PatientDTO mapPatientToPatientDTO(Patient patient) {
        if (patient != null) {
            PatientDTO patientDTO = new PatientDTO();
            patientDTO.setId(patient.getId());
            patientDTO.setFirstName(patient.getFirstName());
            patientDTO.setLastName(patient.getLastName());
            patientDTO.setEmail(patient.getEmail());
            patientDTO.setCity(patient.getCity());
            patientDTO.setCountry(patient.getCountry());
            patientDTO.setStreet(patient.getStreet());
            System.out.println(patient.getFirstName());
            patientDTO.setBirthDay(patient.getBirthDay());
            patientDTO.setImageData(patient.getImageData());
            patientDTO.setDoctors(patient.getDoctors());
            patientDTO.setInquiriesList(getInquiriesOfPatient(patient.getId()));
            patientDTO.setAppointments(getAppointmentsOfPatient(patient.getId()));
            return patientDTO;
        }
        return null;
    }

    @Transactional
    public Patient mapPatientDTOToPatient(PatientDTO patientDTO) {
        Patient patient = new Patient();
        patient.setId(patientDTO.getId());
        patient.setFirstName(patientDTO.getFirstName());
        patient.setLastName(patientDTO.getLastName());
        patient.setEmail(patientDTO.getEmail());
        patient.setCity(patientDTO.getCity());
        patient.setCountry(patientDTO.getCountry());
        patient.setStreet(patientDTO.getStreet());
        patient.setBirthDay(patientDTO.getBirthDay());
        patient.setImageData(patientDTO.getImageData());
        patient.setPassword(patientDTO.getPassword());
        patient.setInquiries(getInquiriesOfPatient(patientDTO.getId()));
        patient.setDoctors(patientDTO.getDoctors());
        patient.setAppointments(getAppointmentsOfPatient(patientDTO.getId()));
        return patient;
    }

    @Transactional
    public PatientDTO createPatient(PatientDTO patientDTO) {
        Patient patient = mapPatientDTOToPatient(patientDTO);
        patientRepository.save(patient);
        Users user = new Users();
        user.setId(patient.getId());
        user.setEmail(patient.getEmail());
        user.setPassword(patient.getPassword());
        List<Role> userRoles = user.getRoles();
        userRoles.add(roleRepository.findByRole("PATIENT"));
        user.setRoles(userRoles);
        usersRepository.save(user);
        return patientDTO;
    }
    @Transactional

    public List<PatientDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findAll();
        List<PatientDTO> patientDTOS = new ArrayList<>();
        for (int i = 0; i < patients.size(); i++) {
            patientDTOS.add(mapPatientToPatientDTO(patients.get(i)));
        }
        return patientDTOS;
    }
    @Transactional

    public PatientDTO getPatientByID(Long id) {
        PatientDTO patientDTO = mapPatientToPatientDTO(patientRepository.findById(id).orElse(null));
        if (patientDTO != null)
            return patientDTO;
        throw new IllegalArgumentException("Patient not found");
    }
    @Transactional
    public PatientDTO getPatientByEmail(String email) {
        PatientDTO patientDTO = mapPatientToPatientDTO(patientRepository.findByEmail(email).orElse(null));
        if (patientDTO != null)
            return patientDTO;
        throw new IllegalArgumentException("Patient not found");
    }
    @Transactional
    public PatientDTO updatePatient(Long id, PatientDTO patientDTO) {
        if (patientDTO == null || patientDTO.getEmail() == null) {
            throw new IllegalArgumentException("Invalid patient data.");
        }
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        patient = updatePatientDetails(patient,patientDTO);
        patient = patientRepository.save(patient);
        return mapPatientToPatientDTO(patient);
    }

    @Transactional
    public Patient updatePatientDetails(Patient patient, PatientDTO patientDTO) {
        patient.setFirstName(patientDTO.getFirstName());
        patient.setLastName(patientDTO.getLastName());
        patient.setCity(patientDTO.getCity());
        patient.setCountry(patientDTO.getCountry());
        patient.setStreet(patientDTO.getStreet());
        patient.setBirthDay(patientDTO.getBirthDay());
        return patient;
    }
    @Transactional
    public void deletePatient(Long id) throws Exception {
        Optional<Patient> optionalPatient = patientRepository.findById(id);
        if (!optionalPatient.isPresent()) {
            throw new Exception("Patient not found with ID: " + id);
        }
        Patient patient = optionalPatient.get();
        deletePatientFromDoctors(id);
        Optional<Users> user = usersRepository.findByEmail(patient.getEmail());
        usersRepository.delete(user.get());
        patientRepository.delete(patient);
    }
    @Transactional

    public List<DoctorDTO> getDoctorsOfPatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + id));

        List<Doctor> doctors = patient.getDoctors();

        return doctors.stream()
                .map(doctorService::mapDoctorToDoctorDTO)
                .collect(Collectors.toList());
    }
    @Transactional
    public List<Inquiries> getInquiriesOfPatient(Long id) {
        List<Inquiries> userInquiries = new ArrayList<>();
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + id));
        List<Inquiries> patientInquiries = patient.getInquiries();
        for (Inquiries inquiry : patientInquiries) {
            Inquiries masterInquiry = inquiriesRepository.findById(inquiry.getId())
                    .orElseThrow(() -> new NoSuchElementException("Inquiry not found with id: " + inquiry.getId()));
            userInquiries.add(masterInquiry);
        }
        System.out.println(userInquiries);
        return userInquiries;
    }

    public List<Appointments> getAppointmentsOfPatient(Long id) {
        List<Appointments> userAppointments = new ArrayList<>();
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + id));
        List<Appointments> patientAppointments = patient.getAppointments();
        for (Appointments appointment : patientAppointments) {
            Appointments masterAppointment = appointmentsRepository.findById(appointment.getId())
                    .orElseThrow(() -> new NoSuchElementException("Appointment not found with id: " + appointment.getId()));
            userAppointments.add(masterAppointment);
        }
        System.out.println(userAppointments);
        return userAppointments;
    }
    @Transactional

    public Appointments createAppointment(Long id, Appointments appointment) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + id));
        List<Appointments> appointments = patient.getAppointments();
        appointments.add(appointment);
        patient.setAppointments(appointments);
        return appointment;
    }

    @Transactional
    public List<DoctorDTO> addDoctorToPatient(Long patientID, Long doctorID) {
        Patient patient = patientRepository.findById(patientID)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + patientID));
        Doctor doctor = doctorRepository.findById(doctorID)
                .orElseThrow(() -> new NoSuchElementException("Doctor not found with id: " + doctorID));
        List<Doctor> doctors = patient.getDoctors();
        doctors.add(doctor);
        patient.setDoctors(doctors);
       List<Patient> patients = doctor.getPatients();
       patients.add(patient);
       doctor.setPatients(patients);
        return doctors.stream().
                map(doctorService::mapDoctorToDoctorDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Appointments> addAppointmentToPatient(Long patientID,Long doctorID, Date date) {
        Patient patient = patientRepository.findById(patientID)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + patientID));
        Date now = new Date();
        Optional<Doctor> doctor = doctorRepository.findById(doctorID);
        if(doctor.isPresent()) {
            for (Appointments doctorAppointments : doctor.get().getAppointments()) {
                if (doctorAppointments.getDate().equals(date)) {
                    throw new NoSuchElementException("The doctor already has appointment in this date");
                }
            }
            if (!date.after(now)) {
                Appointments appointment = new Appointments();
                appointment.setPatient(patient);
                appointment.setDoctor(doctor.get());
                appointment.setDate(date);
                List<Appointments> appointments = patient.getAppointments();
                appointments.add(appointment);
                patient.setAppointments(appointments);
                List<Appointments> appointments2 = doctor.get().getAppointments();
                appointments2.add(appointment);
                doctor.get().setAppointments(appointments2);
                return appointments;
                } else {
                    throw new NoSuchElementException("The date has already passed");
                }
        }
        else{
            throw new NoSuchElementException("There is no such doctor: "+ doctor.get().getFirstName() + doctor.get().getLastName());
        }
    }

    @Transactional
    public List<Inquiries> addInquiryToPatient(Long patientID, Map<String, Object> inquiryData) {
        Map<String, Object> doctorData = (Map<String, Object>) inquiryData.get("doctor");
        Doctor doctor = convertToDoctor(doctorData);

        Map<String, Object> patientData = (Map<String, Object>) inquiryData.get("patient");
        Patient patient = convertToPatient(patientData);

        String symptoms = (String) inquiryData.get("symptoms");

        patient = patientRepository.findById(patientID)
                .orElseThrow(() -> new NoSuchElementException("Patient not found with id: " + patientID));
        Inquiries inquiry = new Inquiries();
        inquiry.setDoctor(doctor);
        inquiry.setPatient(patient);
        inquiry.setSymptoms(symptoms);
        List<Inquiries> inquiries = patient.getInquiries();
        inquiry.setHasAnswered(false);
        Optional<Doctor> doctorOptional = doctorRepository.findById(inquiry.getDoctor().getId());
        if (doctorOptional.isPresent()) {
            doctor = doctorOptional.get();
            List<Inquiries> doctorInquiries = doctor.getInquiries();
            doctorInquiries.add(inquiry);
            doctor.setInquiries(doctorInquiries);
            inquiries.add(inquiry);
            patient.setInquiries(inquiries);
            inquiriesRepository.save(inquiry);
            return inquiries;
        } else {
            throw new NoSuchElementException("Doctor not found with id: " + inquiry.getDoctor().getId());
        }
    }
    @Transactional
    public Doctor convertToDoctor(Map<String, Object> doctorData) {
        Doctor doctor = new Doctor();
        doctor.setId(doctorData.get("id") instanceof Long ? (Long) doctorData.get("id") : ((Integer) doctorData.get("id")).longValue());
        doctor.setFirstName((String) doctorData.get("firstName"));
        doctor.setLastName((String) doctorData.get("lastName"));
        doctor.setEmail((String) doctorData.get("email"));
        doctor.setCity((String) doctorData.get("city"));
        doctor.setCountry((String) doctorData.get("country"));
        doctor.setStreet((String) doctorData.get("street"));
        doctor.setBirthDay((String) doctorData.get("birthDay"));
        doctor.setImageData(parseImageData((String) doctorData.get("imageData")));
        doctor.setPassword((String) doctorData.get("password"));
        doctor.setSpecialization((String) doctorData.get("specialization"));
        doctor.setPatients((List<Patient>) doctorData.get("patients"));
        doctor.setInquiries((List<Inquiries>) doctorData.get("inquiriesList"));
        doctor.setAppointments((List<Appointments>) doctorData.get("appointments"));
        return doctor;
    }

    private byte[] parseImageData(String imageDataString) {
        // Decode Base64 string to byte array
        return Base64.getDecoder().decode(imageDataString);
    }
    @Transactional
    public Patient convertToPatient(Map<String, Object> patientData) {
        Patient patient = new Patient();
        patient.setId(patientData.get("id") instanceof Long ? (Long) patientData.get("id") : ((Integer) patientData.get("id")).longValue());
        patient.setFirstName((String) patientData.get("firstName"));
        patient.setLastName((String) patientData.get("lastName"));
        patient.setEmail((String) patientData.get("email"));
        patient.setCity((String) patientData.get("city"));
        patient.setCountry((String) patientData.get("country"));
        patient.setStreet((String) patientData.get("street"));
        patient.setBirthDay((String) patientData.get("birthDay"));
        patient.setImageData(parseImageData((String) patientData.get("imageData")));
        patient.setPassword((String) patientData.get("password"));
        patient.setInquiries((List<Inquiries>)patientData.get("inquiriesList"));
        patient.setDoctors((List<Doctor>) patientData.get("doctors"));
        patient.setAppointments((List<Appointments>) patientData.get("appointments"));
        return patient;
    }


    @Transactional
    public void deletePatientFromDoctors(Long patientId) {
        List<Doctor> doctors = doctorRepository.findAll();
        for (Doctor doctor : doctors) {
            doctorService.deletePatientFromDoctor(doctor.getId(), patientId);
        }
    }
    @Transactional
    public void deleteDoctorFromPatients(Long doctorId, Long patientId) {
        Optional<Patient> optionalPatient = patientRepository.findById(patientId);

        if (optionalPatient.isPresent()) {
            Patient patient = optionalPatient.get();
            List<Doctor> doctors = patient.getDoctors();

            Iterator<Doctor> iterator = doctors.iterator();
            while (iterator.hasNext()) {
                Doctor doctor = iterator.next();
                if (doctor.getId().equals(doctorId)) {
                    iterator.remove();
                    break;
                }
            }
            patient.setDoctors(doctors);
        }
    }
    @Transactional
    public Users addRoleToPatient(Long id, String roleName){
        Role role = roleRepository.findByRole(roleName);
        System.out.println(roleRepository.findByRole(roleName));
        Optional<Patient> patient = patientRepository.findById(id);
        Optional<Users> user = usersRepository.findByEmail(patient.get().getEmail());
        Users userNew = user.get();
        List<Role> roles = userNew.getRoles();
        roles.add(role);
        userNew.setRoles(roles);
        if(userNew != null){
            usersRepository.save(userNew);
        }
        return userNew;
    }
    public void uploadImage(Long id, MultipartFile file) throws Exception {
        Optional<Patient> patient = patientRepository.findById(id);
        try {
            if (!file.isEmpty()) {
                patient.get().setImageData(file.getBytes());
                patientRepository.save(patient.get());
                System.out.println("File uploaded successfully");
            }

        } catch (IOException ex) {
            throw new Exception("Could not store file " + file, ex);
        }
    }

}
