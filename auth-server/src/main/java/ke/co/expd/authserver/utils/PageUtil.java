package ke.co.expd.authserver.utils;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class PageUtil<T, R> {

    public static <T, R> Page<R> constructCustomPage(Page<T> entityPage, List<R> content) {
        return new Page<R>() {
            @Override
            public int getTotalPages() {
                return entityPage.getTotalPages();
            }

            @Override
            public long getTotalElements() {
                return entityPage.getTotalElements();
            }

            @Override
            public <U> Page<U> map(Function<? super R, ? extends U> converter) {
                throw new UnsupportedOperationException("Not implemented");
            }

            @Override
            public int getNumber() {
                return entityPage.getNumber();
            }

            @Override
            public int getSize() {
                return entityPage.getSize();
            }

            @Override
            public int getNumberOfElements() {
                return entityPage.getNumberOfElements();
            }

            @Override
            public List<R> getContent() {
                return null;
            }

            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public Sort getSort() {
                return entityPage.getSort();
            }

            @Override
            public boolean isFirst() {
                return false;
            }

            @Override
            public boolean isLast() {
                return false;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable nextPageable() {
                return null;
            }

            @Override
            public Pageable previousPageable() {
                return null;
            }
            @Override
            public Iterator<R> iterator() {
                return content.iterator();
            }
        };
    }

}
