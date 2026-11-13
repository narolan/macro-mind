package com.macromind.api;

import com.macromind.domain.model.BuildAdvice;

/** One advice entry as the overlay receives it. The field names are a contract with overlay/src/viewModel.js. */
record AdviceView(String item, int itemId, String reasoning, String priority, String category, String confidence,
                  String target) {

    static AdviceView from(BuildAdvice advice) {
        return new AdviceView(
                advice.recommendedItem().name(),
                advice.recommendedItem().id(),
                advice.reasoning(),
                advice.priority().name(),
                advice.category().name(),
                advice.confidence().name(),
                advice.targetEnemy() == null ? null : advice.targetEnemy().championName());
    }
}
