package vn.edu.hust.soict.judge0client.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import vn.edu.hust.soict.judge0client.config.Judge0Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Judge0Utils {

    Judge0Config judge0Config;

    Pattern LIBS_PATTERN;

    public Judge0Utils(Judge0Config judge0Config) {
        this.judge0Config = judge0Config;
        this.LIBS_PATTERN = Pattern.compile(String.join("|", judge0Config.getMultiThreaded().getMultiThreadedLibs()));
    }

    public boolean isMultiThreadedProgram(int languageId, String sourceCode) {
        Matcher matcher = LIBS_PATTERN.matcher(sourceCode);
        boolean useMultiThreadedLib = matcher.find();

        return (languageId == 71 && useMultiThreadedLib) || languageId == 62;
    }

    public Judge0Config.ServerConfig getServerConfig(int languageId, String sourceCode) {
        if (isMultiThreadedProgram(languageId, sourceCode)) {
            return judge0Config.getMultiThreaded();
        } else {
            return judge0Config.getSingleThreaded();
        }
    }

    public Integer getMaxProcessesAndOrThreads(int languageId, String sourceCode) {
        if (isMultiThreadedProgram(languageId, sourceCode)) {
            return null;
        } else {
            return languageId == 62 ? judge0Config.getMultiThreaded().getSubmission().getJavaMaxProcessesAndOrThreads() : Integer.valueOf(2);
        }
    }
}
