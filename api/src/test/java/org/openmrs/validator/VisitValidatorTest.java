/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.validator;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.test.matchers.HasFieldErrors.hasFieldErrors;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.api.APIException;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.test.jupiter.BaseContextSensitiveTest;
import org.openmrs.util.GlobalPropertiesTestHelper;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class VisitValidatorTest extends BaseContextSensitiveTest {
	
	protected static final String DATA_XML = "org/openmrs/validator/include/VisitValidatorTest.xml";
	
	private GlobalPropertiesTestHelper globalPropertiesTestHelper;
	
	private VisitService visitService;
	
	private Calendar calendar;
	
	private static long DATE_TIME_2014_01_04_00_00_00_0 = 1388790000000L;
	
	private static long DATE_TIME_2014_02_05_00_00_00_0 = 1391554800000L;
	
	private static long DATE_TIME_2014_02_11_00_00_00_0 = 1392073200000L;
	
	@BeforeEach
	public void before() throws ParseException {
		executeDataSet(DATA_XML);
		visitService = Context.getVisitService();
		
		//The only reason for adding the four lines below is because without them,
		//some tests fail on my macbook.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		DATE_TIME_2014_01_04_00_00_00_0 = formatter.parse("2014/01/04").getTime();
		DATE_TIME_2014_02_05_00_00_00_0 = formatter.parse("2014/02/05").getTime();
		DATE_TIME_2014_02_11_00_00_00_0 = formatter.parse("2014/02/11").getTime();
		
		// Do not allow overlapping visits to test full validation of visit start and stop dates.
		//
		globalPropertiesTestHelper = new GlobalPropertiesTestHelper(Context.getAdministrationService());
		globalPropertiesTestHelper.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_ALLOW_OVERLAPPING_VISITS, "false");
		
		calendar = Calendar.getInstance();
	}
	
	@AfterEach
	public void tearDown() {
		globalPropertiesTestHelper.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_ALLOW_OVERLAPPING_VISITS, "true");
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldAcceptAVisitThatHasTheRightNumberOfAttributeOccurrences() {
		int patientId = 42;
		Visit visit = makeVisit(patientId);
		visit.addAttribute(makeAttribute("one"));
		visit.addAttribute(makeAttribute("two"));
		ValidateUtil.validate(visit);
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitIfItHasFewerThanMinOccursOfAnAttribute() {
		Visit visit = makeVisit();
		visit.addAttribute(makeAttribute("one"));
		assertThrows(APIException.class, () -> ValidateUtil.validate(visit));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitIfItHasMoreThanMaxOccursOfAnAttribute() {
		Visit visit = makeVisit();
		visit.addAttribute(makeAttribute("one"));
		visit.addAttribute(makeAttribute("two"));
		visit.addAttribute(makeAttribute("three"));
		visit.addAttribute(makeAttribute("four"));
		assertThrows(APIException.class, () -> ValidateUtil.validate(visit));
	}
	
	private Visit makeVisit() {
		return makeVisit(2);
	}
	
	private Visit makeVisit(Integer patientId) {
		Visit visit = new Visit();
		visit.setPatient(Context.getPatientService().getPatient(patientId));
		visit.setStartDatetime(new Date());
		visit.setVisitType(visitService.getVisitType(1));
		return visit;
	}
	
	private VisitAttribute makeAttribute(Object typedValue) {
		VisitAttribute attr = new VisitAttribute();
		attr.setAttributeType(visitService.getVisitAttributeType(1));
		attr.setValue(typedValue);
		return attr;
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfPatientIsNotSet() {
		VisitService vs = Context.getVisitService();
		Visit visit = new Visit();
		visit.setVisitType(vs.getVisitType(1));
		visit.setStartDatetime(new Date());
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("patient"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfStartDatetimeIsNotSet() {
		VisitService vs = Context.getVisitService();
		Visit visit = new Visit();
		visit.setVisitType(vs.getVisitType(1));
		visit.setPatient(Context.getPatientService().getPatient(2));
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfVisitTypeIsNotSet() {
		Visit visit = new Visit();
		visit.setPatient(Context.getPatientService().getPatient(2));
		visit.setStartDatetime(new Date());
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("visitType"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfTheEndDatetimeIsBeforeTheStartDatetime() {
		Visit visit = new Visit();
		Calendar c = Calendar.getInstance();
		visit.setStartDatetime(c.getTime());
		c.set(2010, 3, 15);//set to an older date
		visit.setStopDatetime(c.getTime());
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("stopDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfTheStartDatetimeIsAfterAnyEncounter() {
		Visit visit = Context.getVisitService().getVisit(1);
		
		Encounter encounter = Context.getEncounterService().getEncounter(3);
		visit.setPatient(encounter.getPatient());
		encounter.setVisit(visit);
		encounter.setEncounterDatetime(visit.getStartDatetime());
		Context.flushSession();
		Context.getEncounterService().saveEncounter(encounter);
		
		//Set visit start date to after the encounter date.
		Date date = new Date(encounter.getEncounterDatetime().getTime() + 1);
		visit.setStartDatetime(date);
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailIfTheStopDatetimeIsBeforeAnyEncounter() {
		Visit visit = Context.getVisitService().getVisit(1);
		
		Encounter encounter = Context.getEncounterService().getEncounter(3);
		visit.setPatient(encounter.getPatient());
		encounter.setVisit(visit);
		encounter.setEncounterDatetime(visit.getStartDatetime());
		Context.flushSession();
		Context.getEncounterService().saveEncounter(encounter);
		
		//Set visit stop date to before the encounter date.
		Date date = new Date(encounter.getEncounterDatetime().getTime() - 1);
		visit.setStopDatetime(date);
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("stopDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	// This test will throw org.hibernate.PropertyValueException: not-null property references a null or transient value: org.openmrs.VisitAttribute.valueReference
	// This is a general problem, i.e. that validators on Customizable can't really be called unless you set Hibernate's flushMode to MANUAL.  
	// Once we figure it out, this test can be un-Ignored
	@Disabled
	public void validate_shouldFailIfAnAttributeIsBad() {
		Visit visit = visitService.getVisit(1);
		visit.addAttribute(makeAttribute(new Date()));
		visit.addAttribute(makeAttribute("not a date"));
		visit.getActiveAttributes();
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("attributes"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitThatStartsBeforeAndEndsAfterAnExistingVisit() {
		int patientId = 42;
		
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		// New visit starts before and ends after the existing visit
		String startDateTime = "2014-01-04T08:00:00";
		String endDateTime = "2014-01-10T16:00:00";
		
		//	Existing visit:  		|---------|
		//	New visit:       |----------------------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */	
	@Test
	public void validate_shouldRejectAVisitThatStartsBeforeAndEndsDuringAnExistingVisit() {
		int patientId = 42;
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		// New visit starts before and ends during the existing visit
		String startDateTime = "2014-01-04T08:00:00";
		String endDateTime = "2014-01-04T12:00:00";
		
		//	Existing event:  	   |----------------------|
		//	New event:       |------------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitThatStartsDuringAndEndsAfterAnExistingVisit() {
		int patientId = 42;
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		// New visit starts during and ends after the existing visit
		String startDateTime = "2014-01-04T12:00:00";
		String endDateTime = "2014-01-04T16:00:00";
		
		//	Existing visit:  	|------------|
		//	New visit:                  |----------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitThatStartsAndEndsDuringAnExistingVisit() {
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		int patientId = 42;
		
		// New visit starts and ends during the existing visit
		String startDateTime = "2014-01-04T12:00:00";
		String endDateTime = "2014-01-04T13:00:00";
		
		// Existing visit:  	|----------------------|
		// New visit:                  |------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitThatStartsAtTheSameTimeAsAnExistingVisit() {
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		int patientId = 42;
		
		// New visit starts at the same time as the existing visit and ends before the existing visit
		String startDateTime = "2014-01-04T10:00:00";
		String endDateTime = "2014-01-04T12:00:00";
		
		// Existing visit:  |----------------------|
		// New visit:       |------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldRejectAVisitThatEndsAtTheSameTimeAsAnExistingEvent(){
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		int patientId = 42;
		
		// New visit starts before the existing visit and ends at the same time as the existing visit
		String startDateTime = "2014-01-04T08:00:00";
		String endDateTime = "2014-01-10T14:00:00";
		
		// Existing visit:  |----------------------|
		// New visit:           |------------------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldRejectOverlappingNewActiveVisit() {
		int patientId = 42;
		
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		// New visit starts before and ends after the existing visit
		String startDateTime = "2014-01-04T12:00:00";
		String endDateTime = null;
		
		//	Existing visit:  |---------|
		//	New visit:            |----------->
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldRejectOverlappingActiveVisits() {
		int patientId = 43;
		
		// Existing visit: starts on 2014-01-04 10:00:00 and no end date (see VisitValidatorTest.xml)
		// New visit starts before and ends after the existing visit
		String startDateTime = "2014-01-04T12:00:00";
		String endDateTime = "2014-01-04T14:00:00";
		
		//	Existing visit:  |---------------->
		//	New visit:            |--------|
		
		Errors errors = validateVisitOverlap(startDateTime, endDateTime, patientId);
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 * Should skip validation if it is an end visit call and start date is not changed
	 */
	@Test
	public void validate_shouldNotRejectOnVisitOverlapDuringEndVisit() {
		
		//	active visit:               |---------------->
		//  overlapping visit:		          |--|
		//  ended active visit:           |----------|
		
		String activeVisitUuid = "c2639863-cbbe-44bb-986d-8a4820f8ae14";
		Visit activeVisit = Context.getVisitService().getVisitByUuid(activeVisitUuid);
		
		String newStopDateTime = "2014-02-06T00:00:00";
		
		// make a clone and update the visit with an end date
		Visit updatedVisit = new Visit();
		updatedVisit.setVisitId(activeVisit.getVisitId());
		updatedVisit.setUuid(activeVisit.getUuid());
		updatedVisit.setPatient(activeVisit.getPatient());
		updatedVisit.setStartDatetime(activeVisit.getStartDatetime());
		updatedVisit.setVisitType(activeVisit.getVisitType());
		updatedVisit.setStopDatetime(parseIsoDate(newStopDateTime));
		
		Errors errors = new BindException(updatedVisit, "visit");
		new VisitValidator().validate(updatedVisit, errors);
		
		assertFalse(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 * Should skip validation if it is an end visit call and start date is not changed
	 */
	@Test
	public void validate_shouldRejectOnVisitOverlapDuringEndVisitWithStartDateUpdated() {
		//	active visit:               |---------------->
		//  overlapping visit:		        |--|
		//  ended active visit:         |----------|
		
		String activeVisitUuid = "c2639863-cbbe-44bb-986d-8a4820f8ae14";
		Visit activeVisit = Context.getVisitService().getVisitByUuid(activeVisitUuid);
		
		String newStartDateTime = "2014-02-04T00:00:00";
		String newStopDateTime = "2014-02-06T00:00:00";
		
		// make a clone and update the visit with an end date
		Visit updatedVisit = new Visit();
		updatedVisit.setVisitId(activeVisit.getVisitId());
		updatedVisit.setUuid(activeVisit.getUuid());
		updatedVisit.setPatient(activeVisit.getPatient());
		updatedVisit.setVisitType(activeVisit.getVisitType());
		updatedVisit.setStartDatetime(parseIsoDate(newStartDateTime));
		updatedVisit.setStopDatetime(parseIsoDate(newStopDateTime));
		
		Errors errors = new BindException(updatedVisit, "visit");
		new VisitValidator().validate(updatedVisit, errors);
		
		assertTrue(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 * This test verifies that updating an existing visit does not result in a rejection 
	 * when there are no other overlapping visits.
	 */
	@Test
	public void validate_shouldNotRejectOnVisitOverlapDuringUpdate() {
		int patientId = 42;
		
		List<Visit> visits = Context.getVisitService()
			.getVisitsByPatient(Context.getPatientService().getPatient(patientId), true, false);
		
		assertFalse(visits.isEmpty());
		
		// Existing visit: starts on 2014-01-04 10:00:00 and ends on 2014-01-10 14:00:00 (see VisitValidatorTest.xml)
		Visit visit = visits.get(0);
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertFalse(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldAcceptAVisitIfStartDateTimeIsEqualToStartDateTimeOfAnotherVoidedVisitOfTheSamePatient()
	        {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(DATE_TIME_2014_02_05_00_00_00_0);
		
		Visit visit = makeVisit(42);
		visit.setStartDatetime(calendar.getTime());
		
		assertTrue(patientHasVoidedVisit(visit.getPatient(), DATE_TIME_2014_02_05_00_00_00_0,
		    DATE_TIME_2014_02_11_00_00_00_0));
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		
		assertFalse(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldAcceptAVisitIfStartDateTimeFallsIntoAnotherVoidedVisitOfTheSamePatient() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2014, Calendar.FEBRUARY, 6);
		
		Visit visit = makeVisit(42);
		visit.setStartDatetime(calendar.getTime());
		
		assertTrue(patientHasVoidedVisit(visit.getPatient(), DATE_TIME_2014_02_05_00_00_00_0,
		    DATE_TIME_2014_02_11_00_00_00_0));
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		
		assertFalse(errors.hasFieldErrors("startDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldAcceptAVisitIfStopDateTimeFallsIntoAnotherVoidedVisitOfTheSamePatient() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2014, Calendar.FEBRUARY, 2);
		
		Visit visit = makeVisit(42);
		visit.setStartDatetime(calendar.getTime());
		
		calendar.set(2014, Calendar.FEBRUARY, 8);
		visit.setStopDatetime(calendar.getTime());
		
		assertTrue(patientHasVoidedVisit(visit.getPatient(), DATE_TIME_2014_02_05_00_00_00_0,
		    DATE_TIME_2014_02_11_00_00_00_0));
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		
		assertFalse(errors.hasFieldErrors("stopDatetime"));
	}
	
	/**
	 * @see VisitValidator#validate(Object, org.springframework.validation.Errors)
	 */
	@Test
	public void validate_shouldAcceptAVisitIfItContainsAnotherVoidedVisitOfTheSamePatient() {
		
		Calendar calendar = Calendar.getInstance();
		calendar.set(2014, Calendar.FEBRUARY, 2);
		
		Visit visit = makeVisit(42);
		visit.setStartDatetime(calendar.getTime());
		
		calendar.set(2014, Calendar.FEBRUARY, 12);
		visit.setStopDatetime(calendar.getTime());
		
		assertTrue(patientHasVoidedVisit(visit.getPatient(), DATE_TIME_2014_02_05_00_00_00_0,
		    DATE_TIME_2014_02_11_00_00_00_0));
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		
		assertFalse(errors.hasFieldErrors("stopDatetime"));
	}
	
	private boolean patientHasVoidedVisit(Patient patient, long startInMillis, long stopInMillis) {
		
		// To get voided visit from the past, both inactive AND voided visits are queried.
		//
		List<Visit> visitList = Context.getVisitService().getVisitsByPatient(patient, true, true);
		for (Visit visit : visitList) {
			if (visit.getStartDatetime() != null && visit.getStartDatetime().getTime() == startInMillis
			        && visit.getStopDatetime() != null && visit.getStopDatetime().getTime() == stopInMillis
			        && visit.getVoided()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldPassValidationIfFieldLengthsAreCorrect() {
		Visit visit = makeVisit(42);
		visit.setVoidReason("voidReason");
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertFalse(errors.hasFieldErrors("voidReason"));
	}
	
	/**
	 * @see VisitValidator#validate(Object,Errors)
	 */
	@Test
	public void validate_shouldFailValidationIfFieldLengthsAreNotCorrect() {
		Visit visit = makeVisit(42);
		visit
		        .setVoidReason("too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text too long text");
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		assertTrue(errors.hasFieldErrors("voidReason"));
	}
	
	@Test
	public void validate_shouldFailValidationIfVisitDateBeforeBirthDate() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(1974, 4, 8);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(false);
		visit.setPatient(patient);
		calendar.set(1974, 4, 7);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, hasFieldErrors("startDatetime", "Visit.startDateCannotFallBeforeTheBirthDateOfTheSamePatient"));
	}
	
	@Test
	public void validate_shouldPassValidationIfVisitDateOnBirthDate() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(1974, 4, 8);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(false);
		visit.setPatient(patient);
		calendar.set(1974, 4, 8);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, not(hasFieldErrors("startDatetime")));
	}
	
	@Test
	public void validate_shouldPassValidationIfVisitDateAfterBirthDate() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(1974, 4, 8);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(false);
		visit.setPatient(patient);
		calendar.set(1974, 4, 9);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, not(hasFieldErrors("startDatetime")));
	}
	
	@Test
	public void validate_shouldPassValidationIfVisitDateOnEstimatedBirthDatesGracePeriod() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(2000, 7, 25);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(true);
		calendar.set(2010, 7, 25);
		patient.setDeathDate(calendar.getTime());
		visit.setPatient(patient);
		calendar.set(1995, 7, 25);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, not(hasFieldErrors("startDatetime")));
	}
	
	@Test
	public void validate_shouldFailValidationIfVisitDateBeforeEstimatedBirthDatesGracePeriod() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(2000, 7, 25);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(true);
		calendar.set(2010, 7, 25);
		patient.setDeathDate(calendar.getTime());
		visit.setPatient(patient);
		calendar.set(1995, 7, 24);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, hasFieldErrors("startDatetime", "Visit.startDateCannotFallBeforeTheBirthDateOfTheSamePatient"));
	}
	
	@Test
	public void validate_shouldPassValidationIfVisitDateOnEstimatedBirthDatesMinimumOneYearGracePeriod() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(2000, 7, 25);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(true);
		calendar.set(2000, 8, 25);
		patient.setDeathDate(calendar.getTime());
		visit.setPatient(patient);
		calendar.set(1999, 7, 25);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, not(hasFieldErrors("startDatetime")));
	}
	
	@Test
	public void validate_shouldFailValidationIfVisitDateBeforeEstimatedBirthDatesMinimumOneYearGracePeriod() {
		Visit visit = new Visit();
		Patient patient = new Patient();
		calendar.set(2000, 7, 25);
		patient.setBirthdate(calendar.getTime());
		patient.setBirthdateEstimated(true);
		calendar.set(2000, 8, 25);
		patient.setDeathDate(calendar.getTime());
		visit.setPatient(patient);
		calendar.set(1999, 7, 24);
		visit.setStartDatetime(calendar.getTime());
		Errors errors = new BindException(visit, "visit");
		
		new VisitValidator().validate(visit, errors);
		
		assertThat(errors, hasFieldErrors("startDatetime", "Visit.startDateCannotFallBeforeTheBirthDateOfTheSamePatient"));
	}
	
	private Date parseIsoDate(String isoDate) {
		LocalDateTime localDateTime = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME);
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}
	
	private Errors validateVisitOverlap(String startDateTime, String endDateTime, int patientId) {
		globalPropertiesTestHelper.setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_ALLOW_OVERLAPPING_VISITS, "false");
		Visit visit = makeVisit(patientId);
		visit.setStartDatetime(parseIsoDate(startDateTime));
		if(endDateTime != null) {
			visit.setStopDatetime(parseIsoDate(endDateTime));
		}
		
		Errors errors = new BindException(visit, "visit");
		new VisitValidator().validate(visit, errors);
		
		return errors;
	}
}
