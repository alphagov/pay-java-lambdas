package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.io.IOException;
import java.util.Set;

public class DeserializeWithValidation extends BeanDeserializer {
    
    static ValidatorFactory validatorFactory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();

    static Validator validator = validatorFactory.getValidator();


    protected DeserializeWithValidation(BeanDeserializerBase src) {
        super(src);
    }

    <T> void validate(T t) {
        Set<ConstraintViolation<T>> violations = validator.validate(t);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Object instance = super.deserialize(jsonParser, deserializationContext);
        validate(instance);
        return instance;
    }
}
