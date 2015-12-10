/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public final class OperationToObservableList<T> {

    public static <T> Func1<Observer<List<T>>, Subscription> toObservableList(Observable<T> that) {
        return new ToObservableList<>(that);
    }

    private static class ToObservableList<T> implements Func1<Observer<List<T>>, Subscription> {

        private final Observable<T> that;

        public ToObservableList(Observable<T> that) {
            this.that = that;
        }

         @Override
        public Subscription call(final Observer<List<T>> observer) {

            return that.subscribe(new Observer<T>() {
                final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<>();
                @Override
                public void onNext(T value) {
                    // onNext can be concurrently executed so list must be thread-safe
                    list.add(value);
                }

                @Override
                public void onError(Exception ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    try {
                        // copy from LinkedQueue to List since ConcurrentLinkedQueue does not implement the List interface
                        ArrayList<T> l = new ArrayList<>(list.size());
                        for (T t : list)
                            l.add(t);

                        // benjchristensen => I want to make this list immutable but some clients are sorting this
                        // instead of using toSortedList() and this change breaks them until we migrate their code.
                        // observer.onNext(Collections.unmodifiableList(l));
                        observer.onNext(l);
                        observer.onCompleted();
                    } catch (Exception e) {
                        onError(e);
                    }

                }
            });
        }
    }
}