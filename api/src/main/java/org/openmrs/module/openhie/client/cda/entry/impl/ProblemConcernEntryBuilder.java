package org.openmrs.module.openhie.client.cda.entry.impl;

import java.util.*;

import org.apache.commons.lang.NotImplementedException;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.openmrs.*;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

public class ProblemConcernEntryBuilder extends EntryBuilderImpl {

    /**
     * Problem concern generate
     *
     * @param data
     * @return
     */
    public Act generate(Condition data) {
        Obs problemObs = findFirstProblemObs(data.getPatient(), data.getConcept());
        Obs stopObs = findLastProblemObs(data.getPatient(), data.getConcept());

        Act retVal = super.createAct(
                x_ActClassDocumentEntryAct.Act,
                x_DocumentActMood.Eventoccurrence,
                Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
                data, problemObs , stopObs );

        // Modofiers
        if (data.getStatus() != null)
            switch (data.getStatus()) {
                case HISTORY_OF:
                    retVal.setStatusCode(ActStatus.Completed);
                    break;
                case ACTIVE:
                    retVal.setStatusCode(ActStatus.Active);
                    break;
                case INACTIVE:
                    retVal.setNegationInd(BL.TRUE);
            }
        else
            retVal.setStatusCode(ActStatus.Active);

        Calendar startTime = Calendar.getInstance(),
                stopTime = Calendar.getInstance();
        startTime.setTime(data.getDateCreated());
        if (data.getVoided() != null)
            stopTime.setTime(data.getDateVoided());
        retVal.setEffectiveTime(new IVL<TS>(new TS(startTime, TS.DAY), data.getDateVoided() != null ? new TS(stopTime) : null));

        // Entry relationship
        Observation concernObs = null;

        // Add an entry relationship of the problem

        if (stopObs != null)
            problemObs = stopObs;

        if (problemObs != null)
            concernObs = super.createObservation(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION),
                    this.m_cdaMetadataUtil.getStandardizedCode(problemObs.getConcept(), CdaHandlerConstants.CODE_SYSTEM_SNOMED, CD.class),
                    problemObs);
        else {
            concernObs = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
            concernObs.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION)));
            concernObs.setCode(new CD<String>("64572001", CdaHandlerConstants.CODE_SYSTEM_SNOMED, "SNOMED", null, "Condition", null));
            concernObs.setStatusCode(ActStatus.Completed);
            concernObs.setEffectiveTime(retVal.getEffectiveTime());
            concernObs.setValue(this.m_cdaMetadataUtil.getStandardizedCode(data.getConcept(), CdaHandlerConstants.CODE_SYSTEM_ICD_10, CD.class));
        }

        retVal.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, concernObs));
        return retVal;
    }

    /**
     * Generate clinical statement for problem concern entry
     */
    @Override
    public ClinicalStatement generate(BaseOpenmrsData data) {
        if (data instanceof Condition)
            return this.generate((Condition) data);
        // TODO DrugOrder types
        throw new NotImplementedException();
    }
}
