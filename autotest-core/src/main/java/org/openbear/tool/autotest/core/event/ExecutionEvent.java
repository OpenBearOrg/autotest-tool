package org.openbear.tool.autotest.core.event;

public sealed interface ExecutionEvent
    permits RunStarted, ScenarioStarted, StepStarted, StepFinished, ScenarioFinished, RunFinished {}
