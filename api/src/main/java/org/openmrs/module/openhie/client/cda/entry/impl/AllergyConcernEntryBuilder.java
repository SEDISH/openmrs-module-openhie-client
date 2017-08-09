package org.openmrs.module.openhie.client.cda.entry.impl;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang.NotImplementedException;
import org.marc.everest.datatypes.*;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Participant2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ParticipantRole;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.PlayingEntity;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.EntityClassRoot;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationType;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

public class AllergyConcernEntryBuilder extends EntryBuilderImpl {

    /**
     * Problem concern generate
     *
     * @param data
     * @return
     */

    public Act generate(Allergy data) {
        Act retVal = super.createAct(
                x_ActClassDocumentEntryAct.Act,
                x_DocumentActMood.Eventoccurrence,
                Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY,
                        CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT), data);

        // Add an entry relationship of the problem
        // Obs problemObs = data.getStartObs();
        //if(data.getStopObs() != null)
        //	problemObs = data.getStopObs();
        // Now for allergy type information

        String typeMnemonic = "", display = "";
        if (data.getAllergenType() != null)
            switch (data.getAllergenType()) {
                case DRUG:
                    typeMnemonic = "D";
                    display = "Drug ";
                    break;
                case ENVIRONMENT:
                    typeMnemonic = "E";
                    display = "Environmental ";
                    break;
                case FOOD:
                    typeMnemonic = "F";
                    display = "Food ";
                    break;
                default:
                    typeMnemonic = "O";
                    display = "Other ";
            }
        else {
            typeMnemonic = "O";
            display = "Other ";
        }
        // Complete the code and assign
        if (data.getSeverity().equals(AllergySeverity.INTOLERANCE)) {
            typeMnemonic += "INT";
            display += "Intolerance";
        } else if (typeMnemonic.equals("O")) {
            typeMnemonic = "ALG";
            display += "Allergy";
        } else {
            typeMnemonic += "ALG";
            display += "Allergy";
        }

        Observation problemObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
        problemObservation.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_ALERT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ALLERGY_AND_INTOLERANCE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION)));
        problemObservation.setId(SET.createSET(new II(this.m_cdaConfiguration.getAllergyRoot(), data.getId().toString())));
        problemObservation.setStatusCode(ActStatus.Completed);
        problemObservation.setEffectiveTime(retVal.getEffectiveTime());
        problemObservation.setCode(new CD<String>(typeMnemonic, CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "Act Code", null, display, null));

        PlayingEntity playingEntity = new PlayingEntity(EntityClassRoot.ManufacturedMaterial);

        if (data.getAllergen() != null) {
            problemObservation.setValue(this.m_cdaMetadataUtil.getStandardizedCode(data.getAllergen().getCodedAllergen(), null, CD.class));
            playingEntity.setCode(this.m_cdaMetadataUtil.getStandardizedCode(data.getAllergen().getCodedAllergen(), null, CE.class));
            super.correctCode(playingEntity.getCode(), CdaHandlerConstants.CODE_SYSTEM_RXNORM, CdaHandlerConstants.CODE_SYSTEM_SNOMED, CdaHandlerConstants.CODE_SYSTEM_ICD_10);
            playingEntity.setName(SET.createSET(new PN(Arrays.asList(new ENXP(data.getAllergen().getNonCodedAllergen())))));
            playingEntity.setName(SET.createSET(new PN(Arrays.asList(new ENXP(data.getAllergen().getNonCodedAllergen())))));
        } else {
            problemObservation.setValue(new CD<String>());
            problemObservation.getValue().setNullFlavor(NullFlavor.Unknown);
            playingEntity.setCode(new CE<String>("413477004", CdaHandlerConstants.CODE_SYSTEM_SNOMED, "SNOMED", null, "Allergen or Pseudoallergen", null));
            playingEntity.setName(SET.createSET(new PN(Arrays.asList(new ENXP("Substance unknown")))));
        }

        // The agent..
        problemObservation.getParticipant().add(new Participant2(ParticipationType.Consumable, ContextControl.OverridingPropagating));
        problemObservation.getParticipant().get(0).setParticipantRole(new ParticipantRole(new CS<String>("MANU")));
        problemObservation.getParticipant().get(0).getParticipantRole().setPlayingEntityChoice(playingEntity);

        // Now the severity
        String severityCode = null;

        if (data.getSeverity() == getConceptByGlobalProperty("allergy.concept.severity.severe"))
            severityCode = "H";
        else if (data.getSeverity() == getConceptByGlobalProperty("allergy.concept.severity.moderate"))
            severityCode = "M";
        else if (data.getSeverity() == getConceptByGlobalProperty("allergy.concept.severity.mild"))
            severityCode = "L";

        // Severity code
        if (severityCode != null) {
            Observation severityObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);

            severityObservation.setId(SET.createSET(new II(UUID.randomUUID())));
            severityObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_SEVERITY_OBSERVATION), new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_SEVERITY_OBSERVATION)));
            severityObservation.setCode(new CD<String>("SEV", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ActCode", null, "Severity", null));
            severityObservation.setText(new ED(data.getSeverity().getDisplayString()));
            severityObservation.setStatusCode(ActStatus.Completed);
            severityObservation.setValue(new CD<String>(severityCode, CdaHandlerConstants.CODE_SYSTEM_OBSERVATION_VALUE, "ObservationValue", null, null, data.getSeverity().getDisplayString()));
            problemObservation.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, BL.TRUE, null, null, null, severityObservation));
        }

        if (data.getReactions() != null) {
            EntryRelationship manifestation = new EntryRelationship(x_ActRelationshipEntryRelationship.MFST, BL.TRUE);
            manifestation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_MANIFESTATION_RELATION)));

            Observation manifestationObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
            manifestationObservation.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_REACTION_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION)));
            manifestationObservation.setCode(new CD<String>("418799008", CdaHandlerConstants.CODE_SYSTEM_SNOMED, "SNOMED", null, "Symptom", null));
            manifestationObservation.setEffectiveTime(this.m_cdaDataUtil.createTS(data.getDateCreated()));
            LIST<CD> listOfReactions = new LIST<CD>();
            for (AllergyReaction reaction : data.getReactions()) {
                listOfReactions.add(this.m_cdaMetadataUtil.getStandardizedCode(reaction.getReaction(), null, CD.class));
            }
            manifestationObservation.setValue(listOfReactions);
            //TODO iterate manifestationObservation value to find instances of CE?
            for (ANY manifestationObservationValue : (LIST<ANY>)manifestationObservation.getValue()) {
                if (manifestationObservationValue instanceof CE)
                    super.correctCode((CE) manifestationObservation.getValue(), CdaHandlerConstants.CODE_SYSTEM_ICD_10, CdaHandlerConstants.CODE_SYSTEM_SNOMED);
            }
            manifestation.setClinicalStatement(manifestationObservation);
            problemObservation.getEntryRelationship().add(manifestation);
        }
        retVal.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.FALSE, BL.TRUE, null, null, null, problemObservation));
        return retVal;
    }

    /**
     * Generate clinical statement for problem concern entry
     */
    @Override
    public ClinicalStatement generate(BaseOpenmrsData data) {
        if (data instanceof Allergy)
            return this.generate((Allergy) data);
        // TODO DrugOrder types
        throw new NotImplementedException();
    }

    private Concept getConceptByGlobalProperty(String globalPropertyName) {
        String globalProperty = Context.getAdministrationService().getGlobalProperty(globalPropertyName);
        Concept concept = Context.getConceptService().getConceptByUuid(globalProperty);
        if (concept == null) {
            throw new IllegalStateException("Configuration required: " + globalPropertyName);
        }
        return concept;
    }
}
