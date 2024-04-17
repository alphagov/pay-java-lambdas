package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class DeserializerModifierWithValidation extends BeanDeserializerModifier {
    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if (deserializer instanceof BeanDeserializer beanDeserializer) {
            return new DeserializeWithValidation(beanDeserializer);
        }
        return deserializer;
    }
    
    public static SimpleModule deserializeAndValidateModule () {
        SimpleModule module = new SimpleModule("DeserializeAndValidate");
        module.setDeserializerModifier(new DeserializerModifierWithValidation());
        return module;
    }
}
