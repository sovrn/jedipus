package com.fabahaba.jedipus.primitive;

import java.util.Queue;
import java.util.function.Function;

import com.fabahaba.jedipus.exceptions.RedisUnhandledException;

class MultiResponseHandler implements Function<Object, Object[]> {

  private final Queue<StatefulFutureReply<?>> multiResponses;

  MultiResponseHandler(final Queue<StatefulFutureReply<?>> responses) {

    this.multiResponses = responses;
  }

  void clear() {

    multiResponses.clear();
  }

  @Override
  public Object[] apply(final Object data) {

    final Object[] inPlaceAdaptedResponses = (Object[]) data;

    if (inPlaceAdaptedResponses.length < multiResponses.size()) {
      throw new RedisUnhandledException(null,
          String.format("Expected to only have %d responses, but was %d.",
              inPlaceAdaptedResponses.length, multiResponses.size()));
    }

    try {
      for (int index = 0;; index++) {
        final StatefulFutureReply<?> response = multiResponses.poll();

        if (response == null) {

          if (index != inPlaceAdaptedResponses.length) {
            throw new RedisUnhandledException(null,
                String.format("Expected to have %d responses, but was only %d.",
                    inPlaceAdaptedResponses.length, index));
          }

          return inPlaceAdaptedResponses;
        }

        response.setMultiResponse(inPlaceAdaptedResponses[index]);
        inPlaceAdaptedResponses[index] = response.get();
      }
    } finally {
      multiResponses.clear();
    }
  }

  void setResponseDependency(final DeserializedFutureRespy<Object[]> dependency) {

    for (final StatefulFutureReply<?> response : multiResponses) {
      response.setDependency(dependency);
    }
  }

  void addResponse(final StatefulFutureReply<?> response) {

    multiResponses.add(response);
  }
}