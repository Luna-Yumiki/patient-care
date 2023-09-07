
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;  
import java.text.*;

import javax.swing.Painter;

import com.github.javafaker.Faker;

import PatientManagement.Catalogs.AgeGroup;
import PatientManagement.Catalogs.VitalSignLimits;
import PatientManagement.Catalogs.VitalSignsCatalog;
import PatientManagement.Clinic.Clinic;
import PatientManagement.Clinic.Event;
import PatientManagement.Clinic.EventSchedule;
import PatientManagement.Clinic.Location;
import PatientManagement.Clinic.LocationList;
import PatientManagement.Clinic.PatientDirectory;
import PatientManagement.Clinic.Site;
import PatientManagement.Clinic.SiteCatalog;
import PatientManagement.Patient.Patient;
import PatientManagement.Patient.Encounters.Encounter;
import PatientManagement.Patient.Encounters.VitalSignMetric;
import PatientManagement.Persona.Person;
import PatientManagement.Persona.PersonDirectory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kal bugrara
 */

public class PatientCareMain {

    /**
     * @param args the command line arguments
     */
    private static Faker MAGIC_BOX = new Faker();
    private static Clinic CLINIC = new Clinic("Northeastern Hospitals");
    private static Scanner READER = new Scanner(System.in);
    public static void main(String[] args) throws ParseException {
        configureVitalSignsCatalog();
        populateData();
        while (true) {
            System.out.println("1. Find sick patients and locations were last seen");
            System.out.println("2. Show infectious patients detail");
            System.out.println("3. Show Infectious and confirmed positive patients detail");
            System.out.println("4. Show abnormal vitals patients detail");
            System.out.println("5. Show infection incidents by location");
            System.out.println("6. Show local mental health care services messages to sick patients");
            System.out.print("Choose option between 1 and 6: ");
            int choice = READER.nextInt();
            if (choice < 1 || choice > 6) {
                System.out.println("Invalid input, retry...");
                continue;
            }
            switch (choice) {
                case 1:
                    findSickPatientsAndLastSeen();
                    break;
                case 2:
                    findInfectiousPatientsDetail();
                    break;
                case 3:
                    findInfectiousPositivePatientsDetail();
                    break;
                case 4:
                    findAbnormalVitalPatientsDetail();
                    break;
                case 5:
                    summarizeInfectiousByLocation();
                    break;
                case 6:
                    localMentalHealthService();
                    break;

            }
        }
    }

    private static void populateData() {
        generateFakePatients(2000);
        generateFakeSites(50);
        generateFakeEvents();
        generateFakeEncounter();
    }

    private static void configureVitalSignsCatalog() {
        VitalSignsCatalog vsc = CLINIC.getVitalSignsCatalog();

        AgeGroup teenager_10_20 = vsc.newAgeGroup("Teenagers 10-20", 20, 10);
        VitalSignLimits heartRateLimits = vsc.newVitalSignLimits("HR");
        VitalSignLimits bloodPressureLimits = vsc.newVitalSignLimits("BP");
        heartRateLimits.addLimits(teenager_10_20, 110, 65);
        bloodPressureLimits.addLimits(teenager_10_20, 145, 75);

        AgeGroup adults_21_50 = vsc.newAgeGroup("Adults 21-50", 50, 21);
        heartRateLimits = vsc.newVitalSignLimits("HR");
        bloodPressureLimits = vsc.newVitalSignLimits("BP");
        heartRateLimits.addLimits(adults_21_50, 105, 55);
        bloodPressureLimits.addLimits(adults_21_50, 140, 70);

        AgeGroup elders_51_80 = vsc.newAgeGroup("Elders 51-80", 80, 51);
        heartRateLimits = vsc.newVitalSignLimits("HR");
        bloodPressureLimits = vsc.newVitalSignLimits("BP");
        heartRateLimits.addLimits(elders_51_80, 100, 50);
        bloodPressureLimits.addLimits(elders_51_80, 135, 65);
    }

    private static void generateFakePatients(int numPatients){
        PersonDirectory pd = CLINIC.getPersonDirectory();
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        for (int i = 0; i < numPatients; i++) {
            // Creating a patient
            String name = MAGIC_BOX.name().name();
            int age = MAGIC_BOX.number().numberBetween(10, 80);
            Person person = pd.newPerson(name, age);
    
            patientDirectory.newPatient(person);
        }
    }

    private static void generateFakeSites(int numSites) {
        for (int i = 0; i < numSites; i++) {
            LocationList locations = CLINIC.getLocationList();
            String address = MAGIC_BOX.address().fullAddress();
            Location location = locations.newLocation(address);
    
            SiteCatalog siteCatalog = CLINIC.getSiteCatalog();
            siteCatalog.newSite(location);
        }
    }

    private static void generateFakeEvents() {
        EventSchedule eventSchedule = CLINIC.getEventSchedule();
        SiteCatalog siteCatalog = CLINIC.getSiteCatalog();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Site site : siteCatalog.getSites()) {
            int eventNum = MAGIC_BOX.number().numberBetween(1, 3);
            for (int i = 0; i < eventNum; i++){
                String dt = sdf.format(MAGIC_BOX.date().past(100, TimeUnit.DAYS)).toString(); // Convert date to string
                eventSchedule.newEvent(site, "0", dt); 
            }
        }   
    }

    private static void generateFakeEncounter() {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        EventSchedule eventSchedule = CLINIC.getEventSchedule();

        for(Patient patient : patientDirectory.getPatients()) {
            String chiefComplaint = MAGIC_BOX.medical().symptoms();
            Encounter encounter = patient.newEncounter(chiefComplaint, eventSchedule.getRandomEvent());
            int hr = MAGIC_BOX.number().numberBetween(40, 120);
            int bp = MAGIC_BOX.number().numberBetween(50, 155);
            encounter.addNewVitals("HR", hr);
            encounter.addNewVitals("BP", bp);
            boolean isInfectious = MAGIC_BOX.bool().bool();//related to question2.
            boolean isConfirmed = MAGIC_BOX.bool().bool();// isConfirmed means the patient is sick, related to question1.
            encounter.newDiagnosis(isInfectious ? "infectious" : "hereditary", isConfirmed);
        }

    }

    private static void findSickPatientsAndLastSeen() {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> confirmedPositivePatients = patientDirectory.getAllConfirmedPositives();
        System.out.println("Below are the confirmed positive patients info and their last seen location:");
        System.out.println("In total "+String.valueOf(confirmedPositivePatients.size())+" patients");
        System.out.println("               Patient ID   Patient Age  Last Seen Location");
        for (Patient patient : confirmedPositivePatients) {
            String patientId = patient.getPerson().getPersonId();
            int patientAge = patient.getPerson().getAge();
            String location = patient.getEncounterHistory().getLastEncounter().getEvent().getSite().getLocation().getStreetName();
            System.out.format("%25s%5d           %32s%n", patientId, patientAge, location);
        }
    }

    private static void findInfectiousPatientsDetail() {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> infectiousPatients = patientDirectory.getAllInfectious();
        System.out.println("Below are the infectious patients info:");
        System.out.println("In total "+String.valueOf(infectiousPatients.size())+" patients");
        System.out.println("               Patient ID   Patient Age  Is Confirmed Positive");
        for (Patient patient : infectiousPatients) {
            String patientId = patient.getPerson().getPersonId();
            int patientAge = patient.getPerson().getAge();
            boolean isConfirmedPositive = patient.isConfirmedPositive();
            System.out.format("%25s%5d           %4b%n", patientId, patientAge, isConfirmedPositive);

        }
    }

    private static void findInfectiousPositivePatientsDetail() {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> patients = patientDirectory.getAllInfectiousAndConfirmedPositive();
        System.out.println("Below are the infectious and confirmed positive patients info:");
        System.out.println("In total "+String.valueOf(patients.size())+" patients");
        System.out.println("               Patient ID   Patient Age  Is Confirmed Positive");
        for (Patient patient : patients) {
            String patientId = patient.getPerson().getPersonId();
            int patientAge = patient.getPerson().getAge();
            boolean isConfirmedPositive = patient.isConfirmedPositive();
            System.out.format("%25s%5d           %4b%n", patientId, patientAge, isConfirmedPositive);

        }
    }

    private static void findAbnormalVitalPatientsDetail() {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> abnormalVitalsPatients = patientDirectory.getAllVitalAbnormal();
        System.out.println("Below are the information of patients whose vitals are abnormal:");
        System.out.println("In total "+String.valueOf(abnormalVitalsPatients.size())+" patients");
        System.out.println("               Patient ID   Patient Age  Last Seen Location");
        for (Patient patient : abnormalVitalsPatients) {
            String patientId = patient.getPerson().getPersonId();
            int patientAge = patient.getPerson().getAge();
            String location = patient.getEncounterHistory().getLastEncounter().getEvent().getSite().getLocation().getStreetName();            
            System.out.format("%25s%5d           %32s%n", patientId, patientAge, location);
        }        
    }

    private static HashMap<String, Integer> summarizeInfectiousByLocationByDate(String cutdate) throws ParseException {
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> patients = patientDirectory.getAllInfectious();
        HashMap<String, Integer> location_summary = new HashMap<String, Integer>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Patient patient : patients) {
            String trackdate = patient.getEncounterHistory().getLastEncounter().getEvent().getDate(); 
            if( sdf.parse(trackdate) .compareTo (sdf.parse(cutdate)) <0 ) {
                String location = patient.getEncounterHistory().getLastEncounter().getEvent().getSite().getLocation().getStreetName();
                if(!location_summary.keySet().contains(location)){
                    location_summary.put(location,1);
                }
                else{
                    location_summary.put(location, location_summary.get(location)+1);
                }
            }
        }
        HashMap<String, Integer> result = sortByValue(location_summary);
        return result;
    }

    private static void summarizeInfectiousByLocation() throws ParseException {
        
        String[] record_dates = new String[] {"2023-01-31", "2023-02-28","2023-03-31","2023-04-30"};
        List<HashMap<String, Integer>> monitoring = new ArrayList<HashMap<String, Integer>>();
        for(int i=0;i<record_dates.length;i++){
            HashMap<String, Integer> summary = summarizeInfectiousByLocationByDate(record_dates[i]);
            monitoring.add(summary);
        }
        
        PatientDirectory patientDirectory = CLINIC.getPatientDirectory();
        List<Patient> patients = patientDirectory.getAllInfectious();
        System.out.println("Below are the summary of infection incidents (infectious patients) by locations :");
        System.out.println("In total "+String.valueOf(patients.size())+" patients");
        System.out.println("Index | Count of Infectious Patients | Location");
        
        HashMap<String, Integer> summary = monitoring.get(record_dates.length-1);
        int index = 0;
        for(String location:summary.keySet()){
            index++;
            System.out.format("%5d%15d                  %32s%n", index, summary.get(location), location);
        }
        
        
        System.out.println("-------------------------------------------------");
        //trace back to fill locations with records in Current_Month but no records in Previous_Month
        
        for(int i=monitoring.size()-1;i>0;i--){
            HashMap<String, Integer> curr_month = monitoring.get(i);
            HashMap<String, Integer> prev_month = monitoring.get(i-1);            
            for(String loc:curr_month.keySet()){
                if(!prev_month.keySet().contains(loc)){
                    prev_month.put(loc,0);
                }
            }
        }

        System.out.println("Below are the summary of infection incidents (infectious patients) by locations over time:");
        System.out.println("In total "+String.valueOf(patients.size())+" patients");
        System.out.println("Index | [Jan, Feb, Mar, Apr] | Location");
        HashMap<String, Integer> trend = monitoring.get(record_dates.length-1);
        int ind = 0;
        for(String location:trend.keySet()){
            int[] history = new int[record_dates.length];
            for(int i=0;i<record_dates.length;i++){
                history[i] = monitoring.get(i).get(location);
            }
            ind++;
            //System.out.format("%12d%5d%5d%5d%5d           %32s%n", ind, history[0], history[1],history[2],history[3],location);
            String message =  Arrays.toString(history) ;
            System.out.format("%5d%22s    %32s%n", ind, message,location);
        }
    }

    private static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Integer>> list =
               new LinkedList<Map.Entry<String, Integer>>(hm.entrySet());
        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Integer> >() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        // put data from sorted list to hashmap
        HashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private static void localMentalHealthService() throws ParseException {
        HashMap<String, Integer> current_month = summarizeInfectiousByLocationByDate("2023-04-30");
        HashMap<String, Integer> previous_month = summarizeInfectiousByLocationByDate("2023-03-31");

        HashMap<String, Integer> risky_zone1 = new HashMap<String, Integer>();
        HashMap<String, Integer> risky_zone2 = new HashMap<String, Integer>();
        HashMap<String, Integer> risky_zone3 = new HashMap<String, Integer>();

        for(String loc: current_month.keySet()){
            if(!previous_month.keySet().contains(loc)){
                risky_zone1.put(loc, current_month.get(loc));
            }
            else{
                int delta = current_month.get(loc)-previous_month.get(loc);
                if(delta>0) {
                    risky_zone2.put(loc, delta);
                }
                else{
                    risky_zone3.put(loc, previous_month.get(loc));
                }
            }
        }

        risky_zone1 = sortByValue(risky_zone1);
        risky_zone2 = sortByValue(risky_zone2);
        risky_zone3 = sortByValue(risky_zone3);

        System.out.println("Here is local mental health care services to help local sick patients, please see your local regions danger level");
        System.out.println("-------------------------------------------------");
        System.out.println("Below regions are danger level 1 - infectious cases found very first time in this month");
        System.out.println("Index | Count of Infectious Patients | Location");
        int index = 0;
        for(String loc:risky_zone1.keySet()){
            int cnt = risky_zone1.get(loc);
            index++;
            System.out.format("%5d%15d                  %32s%n", index, cnt, loc);
        }
        System.out.println("-------------------------------------------------");
        System.out.println("Below regions are danger level 2 - new added infectious cases found in this month");
        System.out.println("Index | Added Cases This Month | Location");
        index = 0;
        for(String loc:risky_zone2.keySet()){
            int cnt = risky_zone2.get(loc);
            index++;
            System.out.format("%5d%15d                  %32s%n", index, cnt, loc);
        }
        System.out.println("-------------------------------------------------");
        System.out.println("Below regions are danger level 3 - no new added infectious cases found in this month");
        System.out.println("Index | Count of Cases Last Month | Location");
        index = 0;
        for(String loc:risky_zone3.keySet()){
            int cnt = risky_zone3.get(loc);
            index++;
            System.out.format("%5d%15d                  %32s%n", index, cnt, loc);
        }
    }
}
