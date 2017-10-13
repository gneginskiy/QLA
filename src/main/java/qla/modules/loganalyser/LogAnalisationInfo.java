package qla.modules.loganalyser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import qla.modules.loganalyser.models.ErrorLine;
import qla.modules.loganalyser.models.LogExeptionModel;
import qla.modules.loganalyser.models.LogModel;
import qla.modules.loganalyser.models.SignalModel;

public class LogAnalisationInfo implements Serializable {
    private static final long serialVersionUID = 5513690698874951557L;
    private String logFile;
    private final Map<Integer, SignalModel> signalModels = new ConcurrentSkipListMap<>();
    private final Map<Integer, LogExeptionModel> exeptionModels = new ConcurrentSkipListMap<>();
    private final Map<Integer, ErrorLine> errorLines = new ConcurrentSkipListMap<>();

    public Collection<SignalModel> getSignalModels() {
        return signalModels.values();
    }

    public Collection<LogExeptionModel> getExeptionModels() {
        return exeptionModels.values();
    }

    public Collection<ErrorLine> getErrorLines() {
        return errorLines.values();
    }

    public SignalModel getSignalModel(int id) {
        return signalModels.get(id);
    }

    public LogExeptionModel getLogExeptionModel(int id) {
        return exeptionModels.get(id);
    }

    public ErrorLine getLogErrorLine(int id) {
        return errorLines.get(id);
    }

    public LogAnalisationInfo addLogModel(LogModel logModel) {
        switch (logModel.getLogModelType()) {
            case EXCEPTION_LOG_MODEL:
                addLogExeptionModel((LogExeptionModel) logModel);
                break;
            case SIGNAL_LOG_MODEL:
                addSignalModel((SignalModel) logModel);
                break;
        }
        return this;
    }

    public synchronized LogAnalisationInfo addSignalModel(SignalModel model) {
        model.setId(model.getLine());
        signalModels.put(model.getLine(), model);
        return this;
    }

    public synchronized LogAnalisationInfo addLogExeptionModel(LogExeptionModel model) {
        model.setId(model.getLine());
        exeptionModels.put(model.getLine(), model);
        return this;
    }

    public LogAnalisationInfo setErrorLine(ErrorLine model) {
        errorLines.put(signalModels.size(), model);
        return this;
    }

    public int getSignalModelsSize() {
        return signalModels.size();
    }

    public int getExeptionModelsSize() {
        return exeptionModels.size();
    }

    public int getErrorLinesSize() {
        return errorLines.size();
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

}
