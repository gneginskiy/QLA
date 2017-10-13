package qla.modules.log.processors;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import qla.modules.confuguration.ConcurrencyHelper;
import qla.modules.log.LogConfiguration;
import qla.modules.log.LogFile;
import qla.modules.log.Logline;
import qla.modules.loganalyser.LogAnalisationInfo;
import qla.modules.loganalyser.models.LogModel;

public class TDPLogProcessor implements ILogProcessor {
	private LogConfiguration logConfiguration;
	private List<ILoglineProcessor> lineProcessors = Collections.synchronizedList(new ArrayList<>());
	private IProcessCallback callback;

	@Override
	public LogAnalisationInfo process(String pathToLogfile) throws IOException {
		long startTimeMillis = System.currentTimeMillis();
		LogFile logFile = new LogFile(pathToLogfile, logConfiguration);
		startProcessing(logFile);
		LogAnalisationInfo analisationInfo = getAnalisationInfo(logFile, lineProcessors, this::progressOfProcessing);
		closeResources(logFile);
		endOfProcessing(logFile);
		printStatistics(startTimeMillis, logFile,analisationInfo);
		return analisationInfo;
	}

	private void closeResources(LogFile logFile)
	{
		logFile.close();
		System.gc();
	}

	private static void printStatistics(long startTimeMillis, LogFile logFile, LogAnalisationInfo analisationInfo)
	{
		double elapsedTime = (System.currentTimeMillis()-startTimeMillis)/1000.;
		double fileSizeMbs = logFile.getFileSize() / 1000000.;
		double mbsPerSecond = fileSizeMbs/elapsedTime;
		int completedLines = analisationInfo.getSignalModels().size();
		int coresUsed = ConcurrencyHelper.getNumberOfCPUCores();
		long maxMemoryMbs  = Runtime.getRuntime().maxMemory()/(1024*1024);
		long freeMemoryMbs = Runtime.getRuntime().freeMemory()/(1024*1024);
		System.err.println("File size: " + fileSizeMbs +
				" mb | Time elapsed " + elapsedTime +
				" | Performance: " + mbsPerSecond+" mb/s" +
				" | Completed lines: " + completedLines +
				" | Cores used: "+ coresUsed +
				" | Free memory: "+ freeMemoryMbs +"/"+maxMemoryMbs);
	}

	private static LogAnalisationInfo getAnalisationInfo(final LogFile logLinesFile,
																			 List<ILoglineProcessor> lineProcessors,
																			 Consumer<LogFile> progressBarChangesConsumer)
	{
		LogAnalisationInfo logAnalisationInfo = new LogAnalisationInfo();

		int nThreads = ConcurrencyHelper.getNumberOfCPUCores();
		ExecutorService service = Executors.newFixedThreadPool(nThreads);
		for (Logline logLine : logLinesFile) {
			Runnable threadTask = () ->	{
				for (ILoglineProcessor iLoglineProcessor : lineProcessors)
				{
					if (iLoglineProcessor.isNeedProcessing(logLine))
					{
						LogModel logModel = iLoglineProcessor.proccess(logLine, logLinesFile);
						logAnalisationInfo.addLogModel(logModel);
						break;
					}
				}
				progressBarChangesConsumer.accept(logLinesFile);
			};
			service.submit(threadTask);
		}

		service.shutdown();
		while (true) {
			if (service.isTerminated()) {
				break;
			}
		}
		return logAnalisationInfo;
	}

	private void endOfProcessing(LogFile logFile) {
		Map<String, Object> data = new HashMap<>();
		data.put("logFile", logFile);
		if(callback != null) {
			callback.endOfProcessing(data);
		}
	}

	private void progressOfProcessing(LogFile logFile) {
		Map<String, Object> data = new HashMap<>();
		data.put("logFile", logFile);
		if(callback != null) {
			callback.progressOfProcessing(data);
		}
	}

	private void startProcessing(LogFile logFile) {
		Map<String, Object> data = new HashMap<>();
		data.put("logFile", logFile);
		if(callback != null) {
			callback.startProcessing(data);
		}
	}

	public void setProcessor(ILoglineProcessor processor) {
		lineProcessors.add(processor);
	}

	public LogConfiguration getLogConfiguration() {
		return logConfiguration;
	}

	public void setLogConfiguration(LogConfiguration logConfiguration) {
		this.logConfiguration = logConfiguration;
	}

	public List<ILoglineProcessor> getLineProcessors() {
		return lineProcessors;
	}

	public void setLineProcessors(List<ILoglineProcessor> lineProcessors) {
		this.lineProcessors = lineProcessors;
	}

	public IProcessCallback getCallback() {
		return callback;
	}

	@Override
	public void setCallback(IProcessCallback callback) {
		this.callback = callback;
	}

}
