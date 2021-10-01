package org.notabarista.storage.api.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentTypeValidator implements ConstraintValidator<ContentType, MultipartFile[]> {

    private Set<String> validContentTypes;

    @Override
    public void initialize(ContentType constraint) {
        validContentTypes = Arrays.stream(constraint.contentTypes()).collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(MultipartFile[] files, ConstraintValidatorContext context) {
        for (MultipartFile file : files) {
            String shortenedContentType = StringUtils.isNotBlank(file.getContentType()) ?
                    file.getContentType().replaceFirst("[^/]*$", "*")
                    : null;
            if (shortenedContentType == null || !validContentTypes.contains(shortenedContentType)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Invalid content type: " + file.getContentType())
                       .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
