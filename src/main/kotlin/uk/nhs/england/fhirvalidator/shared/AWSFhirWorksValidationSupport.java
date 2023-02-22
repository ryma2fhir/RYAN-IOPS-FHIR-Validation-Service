package uk.nhs.england.fhirvalidator.shared;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.*;
import ca.uhn.fhir.rest.param.TokenParam;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.BaseValidationSupportWrapper;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.nhs.england.fhirvalidator.awsProvider.AWSAuditEvent;
import uk.nhs.england.fhirvalidator.awsProvider.AWSQuestionnaire;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class AWSFhirWorksValidationSupport extends BaseValidationSupport implements IValidationSupport {
    private static final Logger ourLog = LoggerFactory.getLogger(CachingValidationSupport.class);
    private AWSQuestionnaire awsQuestionnaire ;
    private AWSAuditEvent awsAuditEvent;

    public AWSFhirWorksValidationSupport(FhirContext theFhirContext, AWSQuestionnaire _awsQuestionnaire, AWSAuditEvent _awsAuditEvent) {
        super(theFhirContext);
        awsQuestionnaire = _awsQuestionnaire;
        awsAuditEvent = _awsAuditEvent;
    }

    @Override
    public List<IBaseResource> fetchAllConformanceResources() {
        ArrayList<IBaseResource> retVal = new ArrayList<>();
        List<Questionnaire> questionnaires = awsQuestionnaire.search(null);
        for (Questionnaire questionnaire : questionnaires) {
            retVal.add(questionnaire);
        }
        return retVal;
    }

    public <T extends IBaseResource> T fetchResource(@Nullable Class<T> theClass, String theUri) {
        List<Questionnaire> foundQuestionnaire = awsQuestionnaire.search(new TokenParam().setValue(theUri));
        if (foundQuestionnaire.size()>0) return (T) foundQuestionnaire.get(0) ;
        return null;
    }


}
