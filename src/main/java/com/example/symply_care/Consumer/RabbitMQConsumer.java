package com.example.symply_care.Consumer;

import com.example.symply_care.controller.DoctorController;
import com.example.symply_care.controller.EmailSendController;
import com.example.symply_care.controller.PatientController;
import com.example.symply_care.dto.DoctorDTO;
import com.example.symply_care.dto.PatientDTO;
import com.example.symply_care.entity.RabbitMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.util.Objects;

@Service
public class RabbitMQConsumer {
    private final EmailSendController emailSendController;
    private final PatientController patientController;
    private final DoctorController doctorController;


    public RabbitMQConsumer(EmailSendController emailSendController, PatientController patientController, DoctorController doctorController) {
        this.emailSendController = emailSendController;
        this.patientController = patientController;
        this.doctorController = doctorController;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConsumer.class);

    @Transactional
    @RabbitListener(queues = {"${rabbitmq.queue.name}"})
    public void consume(RabbitMQMessage rabbitMQMessage) throws Exception {
        LOGGER.info(String.format("Recieved message -> %s", rabbitMQMessage.toString()));
        if(rabbitMQMessage.getEmail()==null){
        // Get doctor info
        ResponseEntity<DoctorDTO> doctorResponseEntity = doctorController.getDoctorByEmail(rabbitMQMessage.getDoctorEmail());
        DoctorDTO doctor = doctorResponseEntity.getBody();

        // Check for appointment
        if (rabbitMQMessage.getAppointmentDate() != null) {
            sendAppointmentEmail(rabbitMQMessage, doctor);
        } else if (rabbitMQMessage.getDoctorAnswer() == null) {
            sendInquiryEmail(rabbitMQMessage, doctor);
        } else {
            sendAnswerEmail(rabbitMQMessage, doctor);
        }
    }
    else{
        sendMessageToAdmin(rabbitMQMessage);
    }
    }

    private void sendAppointmentEmail(RabbitMQMessage rabbitMQMessage, DoctorDTO doctor) {
        try {
            ResponseEntity<PatientDTO> patientResponseEntity = patientController.getPatientByEmail(rabbitMQMessage.getPatientEmail());
            PatientDTO patient = patientResponseEntity.getBody();

            String subject = "New Appointment Scheduled for You!";
            String body = "<html>" +
                    "<body style=\"font-family: Arial, sans-serif;\">" +
                    "<div style=\"background-color: #f2f2f2; padding: 20px;\">" +
                    "<h1 style=\"color:royalblue; margin-bottom: 20px;\">Hi! It's SYMPly - Care</h1>" +
                    "<p style=\"margin-bottom: 20px;\">We are pleased to inform you that a new meeting has been scheduled for you with the following details:</p>" +
                    "<ul style=\"margin-left: 40px;\">" +
                    "<li style=\"margin-bottom: 10px;\"><strong>Doctor:</strong> " + doctor.getFirstName() + " " + doctor.getLastName() + "</li>" +
                    "<li style=\"margin-bottom: 10px;\"><strong>Patient:</strong> " + patient.getFirstName() + " " + patient.getLastName() + "</li>" +
                    "</ul>" +
                    "<p style=\"margin-bottom: 20px;\">For more details, please check your profile on our website.</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            emailSendController.sendEmail(rabbitMQMessage.getPatientEmail(), subject, body, null);
            emailSendController.sendEmail(rabbitMQMessage.getDoctorEmail(), subject, body, null);
        } catch (Exception e) {
            LOGGER.error("Error sending appointment email: " + e.getMessage());
        }
    }

    private void sendInquiryEmail(RabbitMQMessage rabbitMQMessage, DoctorDTO doctor) throws Exception {
        PatientDTO patient = null;
        DoctorDTO doctor2 = null;
        String recipientEmail = null;
        String recipientName = null;
        if (rabbitMQMessage.getPatientEmail() != null) {
            ResponseEntity<PatientDTO> patientResponseEntity = patientController.getPatientByEmail(rabbitMQMessage.getPatientEmail());
            patient = patientResponseEntity.getBody();
        } else {
            ResponseEntity<DoctorDTO> doctor2ResponseEntity = doctorController.getDoctorByEmail(rabbitMQMessage.getDoctor2Email());
            doctor2 = doctor2ResponseEntity.getBody();
        }
        try {
            String senderInquiryEmail = rabbitMQMessage.getSenderInquiryEmail();
            String senderName = (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctorEmail())) ?
                    "Doctor" : "Patient";
            if (patient != null) {
                recipientEmail = (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctorEmail())) ?
                        rabbitMQMessage.getPatientEmail() : rabbitMQMessage.getDoctorEmail();
                recipientName = (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctorEmail())) ?
                        patient.getFirstName() + " " + patient.getLastName() : doctor.getFirstName() + " " + doctor.getLastName();
            } else {
                recipientEmail = rabbitMQMessage.getDoctor2Email();
                recipientName = doctor2.getFirstName() + " " + doctor2.getLastName();
            }

            String subject = "New Inquiry is waiting for your response!";
            String body = "<html><body><h1 style=\"color:royalblue;\">Hi! it's SYMPly - Care</h1><br/><br/>"
                    + "<p>A new inquiry is waiting for you with these details:</p>"
                    + "<p>From " + senderName + ": " + (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctorEmail()) ? doctor.getFirstName() + " " + doctor.getLastName() : patient.getFirstName() + " " + patient.getLastName()) + "</p>"
                    + "<p>To you: " + recipientName + "</p>"
                    + "<p>For more details, check the website in your profile.</p>"
                    + "</body></html>";

            emailSendController.sendEmail(recipientEmail, subject, body, null);
        } catch (Exception e) {
            LOGGER.error("Error sending inquiry email: " + e.getMessage());
        }
    }



    private void sendAnswerEmail(RabbitMQMessage rabbitMQMessage, DoctorDTO doctor) {
        String recipientEmail = null;
        String recipientName = null;
        PatientDTO patient = null;
        DoctorDTO doctor2 = null;
        try {
            String senderInquiryEmail = rabbitMQMessage.getSenderInquiryEmail();
            if (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctor2Email())) {
                recipientEmail = rabbitMQMessage.getDoctorEmail();
                recipientName = doctor.getFirstName() + " " + doctor.getLastName();
                ResponseEntity<DoctorDTO> doctor2ResponseEntity = doctorController.getDoctorByEmail(rabbitMQMessage.getDoctor2Email());
                doctor2 = doctor2ResponseEntity.getBody();
            } else {
                ResponseEntity<PatientDTO> patientResponseEntity = patientController.getPatientByEmail(rabbitMQMessage.getPatientEmail());
                patient = patientResponseEntity.getBody();
                recipientEmail = rabbitMQMessage.getPatientEmail();
                recipientName = patient.getFirstName() + " " + patient.getLastName();
            }
            String subject = "Your inquiry has been answered!";
            String body = "<html><body><h1 style=\"color:royalblue;\">Hi! it's SYMPly - Care</h1><br/><br/>"
                    + "<p>A new inquiry has been answered with these details:</p>"
                    + "<p>Doctor: " + (Objects.equals(senderInquiryEmail, rabbitMQMessage.getDoctor2Email()) ?
                    doctor2.getFirstName() + " " + doctor2.getLastName() : doctor.getFirstName() + " " + doctor.getLastName()) + "</p>"
                    + "<p>To you: " + recipientName + "</p>"
                    + "<p>For more details, check the website in your profile.</p>"
                    + "</body></html>";

            emailSendController.sendEmail(recipientEmail, subject, body, null);
        } catch (Exception e) {
            LOGGER.error("Error sending answer email: " + e.getMessage());
        }
    }
    private void sendMessageToAdmin(RabbitMQMessage rabbitMQMessage) {
        try {
            // Decode the message
            String decodedMessage = URLDecoder.decode(rabbitMQMessage.getMessage(), "UTF-8");

            String subject = "Hello Ezra, You have received a message from someone interested in contacting you regarding your application.";
            String body = "<html><body><h1 style=\"color:royalblue;\">Hi! it's SYMPly - Care</h1><br/><br/>"
                    + "<p>Information:</p>"
                    + "<p>Message: " + decodedMessage + "</p>"
                    + "<p>From this Email Address: " + rabbitMQMessage.getEmail() + "</p>"
                    + "<p>For more details, check the website in your profile.</p>"
                    + "</body></html>";

            // Send the email with the decoded message
            emailSendController.sendEmail(rabbitMQMessage.getAdminEmail(), subject, body, null);
        } catch (Exception e) {
            LOGGER.error("Error sending answer email: " + e.getMessage());
        }
    }


}
