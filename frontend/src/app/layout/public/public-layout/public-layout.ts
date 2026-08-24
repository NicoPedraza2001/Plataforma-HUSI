import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter, map, startWith } from 'rxjs';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.css',
  host: { '[class.inverted]': 'inverted()' },
})
export class PublicLayout {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /** Las rutas con data.theme === 'invert' pintan el header y el footer al revés. */
  protected readonly inverted = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      startWith(null),
      // Al construirse el layout la ruta hija todavía puede no tener snapshot,
      // por eso se accede de forma defensiva: NavigationEnd corrige el valor.
      map(() => this.route.firstChild?.snapshot?.data?.['theme'] === 'invert'),
    ),
    { initialValue: false },
  );
}
